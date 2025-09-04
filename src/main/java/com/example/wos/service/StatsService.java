package com.example.wos.service;

import com.example.wos.dto.CountDTO;
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

    // subjectContent filtresi için toplam
    public CountDTO countBySubjectContent(String subjectContent) {
        List<Document> pipeline = new ArrayList<>();
        pipeline.add(new Document("$unwind", "$Data.Records.records.REC"));

        if (subjectContent != null && !subjectContent.isBlank()) {
            pipeline.add(new Document("$match",
                new Document("Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.content",
                    new Document("$regex", subjectContent).append("$options", "i"))));
        }

        pipeline.add(new Document("$count", "total"));
        List<Document> out = mongo.getDb().getCollection("data").aggregate(pipeline).into(new ArrayList<>());
        long total = out.isEmpty() ? 0 : ((Number) out.get(0).get("total")).longValue();
        return new CountDTO(total);
    }
}
