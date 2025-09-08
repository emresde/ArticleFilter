package com.example.wos.repo;

import com.example.wos.model.WosData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WosRepository extends MongoRepository<WosData, String> {

    // Subject content içeren kayıtları getir
    @Query("{ 'Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.content' : { $regex: ?0, $options: 'i' } }")
    List<WosData> findBySubjectContent(String subjectContent);

    // Subject content içeren kayıtların sayısını getir
    @Query(value="{ 'Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.content' : { $regex: ?0, $options: 'i' } }", count = true)
    long countBySubjectContent(String subjectContent);
}
