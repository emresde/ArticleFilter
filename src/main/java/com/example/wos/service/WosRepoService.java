package com.example.wos.service;

import com.example.wos.dto.AscatypeCountDTO;
import com.example.wos.model.WosData;
import com.example.wos.repo.WosRepository;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WosRepoService {

    private final WosRepository repo;
    private final MongoTemplate mongo;

    public List<WosData> getBySubjectContent(String subject) {
        return repo.findBySubjectContent(subject);
    }

    public long countBySubject(String subject) {
        return repo.countBySubjectContent(subject);
    }

    public List<WosData> getBySubjectAndAscatype(String subject, String ascatype) {
        return repo.findBySubjectAndAscatype(subject, ascatype);
    }

    // SubjectContent'e göre, ascatype (extended/traditional) kırılımıyla sayım
    public List<AscatypeCountDTO> countBySubjectGroupedByAscatype(String subjectContent) {
        List<Document> p = new ArrayList<>();
        p.add(new Document("$unwind", "$Data.Records.records.REC"));
        p.add(new Document("$unwind", "$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects"));
        p.add(new Document("$unwind", "$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject"));

        if (subjectContent != null && !subjectContent.isBlank()) {
            p.add(new Document("$match", new Document(
                    "Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.content",
                    new Document("$regex", subjectContent).append("$options", "i")
            )));
        }

        p.add(new Document("$group", new Document("_id",
                "$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.ascatype")
                .append("count", new Document("$sum", 1))));
        p.add(new Document("$sort", new Document("count", -1)));

        List<Document> out = mongo.getDb().getCollection("data").aggregate(p).into(new ArrayList<>());
        List<AscatypeCountDTO> res = new ArrayList<>();
        for (Document d : out) {
            String key = d.getString("_id");
            if (key == null) key = "unknown";
            res.add(new AscatypeCountDTO(key, ((Number) d.get("count")).longValue()));
        }
        return res;
    }
}
