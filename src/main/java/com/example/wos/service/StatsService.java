package com.example.wos.service;

import com.example.wos.dto.CountDTO;
import com.example.wos.dto.SubjectCountDTO;
import com.example.wos.dto.SubjectTypeCountDTO;
import com.example.wos.dto.YearCountDTO;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final MongoTemplate mongo;

    public CountDTO countBySubjectContent(String subjectContent) {
        List<Document> p = new ArrayList<>();
        p.add(new Document("$unwind", "$Data.Records.records.REC"));
        if (subjectContent != null && !subjectContent.isBlank()) {
            p.add(new Document("$match",
                    new Document("Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.content",
                            new Document("$regex", subjectContent).append("$options", "i"))));
        }
        p.add(new Document("$count", "total"));
        List<Document> out = mongo.getDb().getCollection("data").aggregate(p).into(new ArrayList<>());
        long total = out.isEmpty() ? 0 : ((Number) out.get(0).get("total")).longValue();
        return new CountDTO(total);
    }

    public List<SubjectCountDTO> allSubjectsCount(String ascatype) {
        List<Document> p = new ArrayList<>();
        p.add(new Document("$unwind", "$Data.Records.records.REC"));
        p.add(new Document("$unwind", "$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects"));
        p.add(new Document("$unwind", "$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject"));

        if (ascatype != null && !ascatype.isBlank()) {
            p.add(new Document("$match", new Document(
                    "Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.ascatype",
                    new Document("$regex", ascatype).append("$options", "i")
            )));
        }

        p.add(new Document("$group", new Document("_id",
                "$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.content")
                .append("count", new Document("$sum", 1))));
        p.add(new Document("$sort", new Document("count", -1)));

        List<Document> out = mongo.getDb().getCollection("data").aggregate(p).into(new ArrayList<>());
        List<SubjectCountDTO> res = new ArrayList<>();
        for (Document d : out) {
            res.add(new SubjectCountDTO(d.getString("_id"), ((Number) d.get("count")).longValue()));
        }
        return res;
    }

    public List<YearCountDTO> histogramByYear(String subjectContent) {
        List<Document> p = new ArrayList<>();
        p.add(new Document("$unwind", "$Data.Records.records.REC"));

        if (subjectContent != null && !subjectContent.isBlank()) {
            p.add(new Document("$match",
                    new Document("Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.content",
                            new Document("$regex", subjectContent).append("$options", "i"))));
        }

        p.add(new Document("$group", new Document("_id",
                "$Data.Records.records.REC.static_data.summary.pub_info.pubyear")
                .append("count", new Document("$sum", 1))));
        p.add(new Document("$sort", new Document("_id", 1)));

        List<Document> out = mongo.getDb().getCollection("data").aggregate(p).into(new ArrayList<>());
        List<YearCountDTO> res = new ArrayList<>();
        for (Document d : out) {
            Integer y = d.getInteger("_id");
            long c = ((Number) d.get("count")).longValue();
            res.add(new YearCountDTO(y, c));
        }
        return res;
    }

    public List<SubjectCountDTO> totalsByAscatype() {
        List<Document> p = new ArrayList<>();
        p.add(new Document("$unwind", "$Data.Records.records.REC"));
        p.add(new Document("$unwind", "$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects"));
        p.add(new Document("$unwind", "$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject"));
        p.add(new Document("$group", new Document("_id",
                "$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.ascatype")
                .append("count", new Document("$sum", 1))));
        p.add(new Document("$sort", new Document("count", -1)));

        List<Document> out = mongo.getDb().getCollection("data").aggregate(p).into(new ArrayList<>());
        List<SubjectCountDTO> res = new ArrayList<>();
        for (Document d : out) {
            String key = d.getString("_id");
            if (key == null) key = "unknown";
            res.add(new SubjectCountDTO(key, ((Number) d.get("count")).longValue()));
        }
        return res;
    }

    public List<SubjectCountDTO> subjectAscatypeBreakdown(String subjectContent) {
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
        List<SubjectCountDTO> res = new ArrayList<>();
        for (Document d : out) {
            String key = d.getString("_id");
            if (key == null) key = "unknown";
            res.add(new SubjectCountDTO(key, ((Number) d.get("count")).longValue()));
        }
        return res;
    }

    public CountDTO countArticlesByAuthorCount(String ascatype, String subjectContent, Integer lt, Integer gte) {
        List<Document> p = baseAuthorCountPipeline(ascatype, subjectContent);

        Document authorCond = new Document();
        if (lt != null) authorCond.append("$lt", lt);
        if (gte != null) authorCond.append("$gte", gte);
        if (!authorCond.isEmpty()) {
            p.add(new Document("$match", new Document("authorCount", authorCond)));
        }

        p.add(new Document("$count", "total"));

        List<Document> out = mongo.getDb().getCollection("data").aggregate(p).into(new ArrayList<>());
        long total = out.isEmpty() ? 0 : ((Number) out.get(0).get("total")).longValue();
        return new CountDTO(total);
    }

    /** subject+ascatype kırılımı */
    public List<SubjectTypeCountDTO> subjectDistributionForAuthorCount(
            String ascatype, String subjectContent, Integer lt, Integer gte, boolean exact) {

        List<Document> p = baseAuthorCountPipeline(ascatype, subjectContent);

        Document authorCond = new Document();
        if (lt != null) authorCond.append("$lt", lt);
        if (gte != null) authorCond.append("$gte", gte);
        if (!authorCond.isEmpty()) {
            p.add(new Document("$match", new Document("authorCount", authorCond)));
        }

        p.add(new Document("$unwind", "$subjects"));

        List<Document> subjectElemAnd = new ArrayList<>();
        if (subjectContent != null && !subjectContent.isBlank()) {
            if (exact) {
                subjectElemAnd.add(new Document("subjects.content", subjectContent));
            } else {
                subjectElemAnd.add(new Document("subjects.content",
                        new Document("$regex", subjectContent).append("$options", "i")));
            }
        }
        if (ascatype != null && !ascatype.isBlank()) {
            subjectElemAnd.add(new Document("subjects.ascatype",
                    new Document("$regex", ascatype).append("$options", "i")));
        }
        if (!subjectElemAnd.isEmpty()) {
            p.add(new Document("$match", new Document("$and", subjectElemAnd)));
        }

        p.add(new Document("$group", new Document("_id", new Document()
                .append("subject", "$subjects.content")
                .append("ascatype", "$subjects.ascatype"))
                .append("count", new Document("$sum", 1))));
        p.add(new Document("$sort", new Document("count", -1)));

        List<Document> out = mongo.getDb().getCollection("data").aggregate(p).into(new ArrayList<>());
        List<SubjectTypeCountDTO> res = new ArrayList<>();
        for (Document d : out) {
            Document id = (Document) d.get("_id");
            String subj = id != null ? id.getString("subject") : null;
            String typ  = id != null ? id.getString("ascatype") : null;
            if (typ == null) typ = "unknown";
            long cnt = ((Number) d.get("count")).longValue();
            res.add(new SubjectTypeCountDTO(subj, typ, cnt));
        }
        return res;
    }

    private List<Document> baseAuthorCountPipeline(String ascatype, String subjectContent) {
        List<Document> p = new ArrayList<>();
        p.add(new Document("$unwind", "$Data.Records.records.REC"));

        Document elem = new Document();
        if (subjectContent != null && !subjectContent.isBlank()) {
            elem.append("content", new Document("$regex", subjectContent).append("$options", "i"));
        }
        if (ascatype != null && !ascatype.isBlank()) {
            elem.append("ascatype", new Document("$regex", ascatype).append("$options", "i"));
        }
        if (!elem.isEmpty()) {
            p.add(new Document("$match", new Document(
                "Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject",
                new Document("$elemMatch", elem)
            )));
        }

        p.add(new Document("$unwind", new Document("path",
                "$Data.Records.records.REC.static_data.fullrecord_metadata.addresses.address_name")
                .append("preserveNullAndEmptyArrays", false)));

        p.add(new Document("$match", new Document(
                "Data.Records.records.REC.static_data.fullrecord_metadata.addresses.address_name.names.name.role", "author"
        )));

        Document authorId = new Document("$ifNull", List.of(
                "$Data.Records.records.REC.static_data.fullrecord_metadata.addresses.address_name.names.name.data-item-ids.data-item-id.content",
                "$Data.Records.records.REC.static_data.fullrecord_metadata.addresses.address_name.names.name.full_name"
        ));

        p.add(new Document("$group", new Document("_id",
                "$Data.Records.records.REC.UID")
                .append("authorsSet", new Document("$addToSet", authorId))
                .append("subjects", new Document("$first",
                        "$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject"))
        ));

        p.add(new Document("$addFields", new Document("authorCount",
                new Document("$size", "$authorsSet")
        )));

        return p;
    }
}
