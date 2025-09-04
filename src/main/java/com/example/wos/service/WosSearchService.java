package com.example.wos.service;

import com.example.wos.dto.RecDTO;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WosSearchService {

    private final MongoTemplate mongo;

    public List<RecDTO> search(Integer year, String q, String doi, String subject, String subjectContent, int page, int size) {
        List<Document> pipeline = new ArrayList<>();

        // 1) REC[]'i aç
        pipeline.add(new Document("$unwind", "$Data.Records.records.REC"));

        // 2) Ön filtreler
        List<Document> and = new ArrayList<>();
        if (year != null) {
            and.add(new Document("Data.Records.records.REC.static_data.summary.pub_info.pubyear", year));
        }
        if (doi != null && !doi.isBlank()) {
            and.add(new Document("Data.Records.records.REC.dynamic_data.cluster_related.identifiers.identifier",
                    new Document("$elemMatch",
                            new Document("type", "doi").append("value", doi))));
        }
        if (subject != null && !subject.isBlank()) {
            and.add(new Document("Data.Records.records.REC.static_data.summary.titles.title",
                    new Document("$elemMatch", new Document("type", "source")
                            .append("content", new Document("$regex", subject).append("$options", "i")))));
        }
        if (subjectContent != null && !subjectContent.isBlank()) {
            and.add(new Document("Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.content",
                    new Document("$regex", subjectContent).append("$options", "i")));
        }
        if (!and.isEmpty()) {
            pipeline.add(new Document("$match", new Document("$and", and)));
        }

        // 3) Null-safe dizi girdileri
        Document safeTitles = new Document("$ifNull", Arrays.asList(
                "$Data.Records.records.REC.static_data.summary.titles.title",
                List.of()
        ));
        Document safeIds = new Document("$ifNull", Arrays.asList(
                "$Data.Records.records.REC.dynamic_data.cluster_related.identifiers.identifier",
                List.of()
        ));

        // 4) $filter ile item-title ve doi listeleri
        Document titleFilter = new Document("$filter", new Document()
                .append("input", safeTitles)
                .append("as", "t")
                .append("cond", new Document("$eq", List.of("$$t.type", "item"))));

        Document doiFilter = new Document("$filter", new Document()
                .append("input", safeIds)
                .append("as", "id")
                .append("cond", new Document("$eq", List.of("$$id.type", "doi"))));

        // 5) Projeksiyon
        pipeline.add(new Document("$project", new Document()
                .append("_id", 0)
                .append("uid", "$Data.Records.records.REC.UID")
                .append("year", "$Data.Records.records.REC.static_data.summary.pub_info.pubyear")
                .append("titleArr", titleFilter)
                .append("doiArr", doiFilter)
        ));

        // 6) Dizilerden ilk elemanı al
        pipeline.add(new Document("$addFields", new Document()
                .append("title", new Document("$cond", Arrays.asList(
                        new Document("$gt", Arrays.asList(new Document("$size", "$titleArr"), 0)),
                        new Document("$arrayElemAt", Arrays.asList("$titleArr.content", 0)),
                        null
                )))
                .append("doi", new Document("$cond", Arrays.asList(
                        new Document("$gt", Arrays.asList(new Document("$size", "$doiArr"), 0)),
                        new Document("$arrayElemAt", Arrays.asList("$doiArr.value", 0)),
                        null
                )))
        ));

        // 7) q varsa title üstünden case-insensitive filtre
        if (q != null && !q.isBlank()) {
            pipeline.add(new Document("$match", new Document("title",
                    new Document("$regex", q).append("$options", "i"))));
        }

        // 8) Son proje + sayfalama
        pipeline.add(new Document("$project", new Document()
                .append("uid", 1).append("year", 1).append("title", 1).append("doi", 1)));

        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        pipeline.add(new Document("$skip", page * size));
        pipeline.add(new Document("$limit", size));

        List<Document> docs = mongo.getDb().getCollection("data").aggregate(pipeline).into(new ArrayList<>());

        List<RecDTO> out = new ArrayList<>();
        for (Document d : docs) {
            out.add(RecDTO.builder()
                    .uid(d.getString("uid"))
                    .year(d.getInteger("year"))
                    .title((String) d.get("title"))
                    .doi((String) d.get("doi"))
                    .build());
        }
        return out;
    }
}
