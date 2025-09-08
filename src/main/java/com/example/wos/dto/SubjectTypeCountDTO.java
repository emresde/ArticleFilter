package com.example.wos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectTypeCountDTO {
    private String subject;   // subject.content
    private String ascatype;  // subject.ascatype (extended/traditional/unknown)
    private long count;       // adet
}
