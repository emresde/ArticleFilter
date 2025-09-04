package com.example.wos.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class RecDTO {
    private String uid;
    private Integer year;
    private String title; // titles[type=item]
    private String doi;   // identifiers[type=doi]
}
