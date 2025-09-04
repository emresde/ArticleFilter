package com.example.wos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@RequiredArgsConstructor
@Document(collection = "data")
public class WosData {

    @Id
    private String id;

    @CreatedDate
    private String createdAt = LocalDateTime.now().toString();

    @LastModifiedDate
    private String modifiedAt = LocalDateTime.now().toString();

    @JsonProperty("Data")
    private Object Data;

    @JsonProperty("QueryResult")
    private Object QueryResult;

    @Transient
    private int recordFound;
}
