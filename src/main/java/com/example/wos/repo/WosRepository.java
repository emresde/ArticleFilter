package com.example.wos.repo;

import com.example.wos.model.WosData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WosRepository extends MongoRepository<WosData, String> {
}
