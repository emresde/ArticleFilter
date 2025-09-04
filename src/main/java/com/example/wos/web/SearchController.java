package com.example.wos.web;

import com.example.wos.dto.RecDTO;
import com.example.wos.service.WosSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class SearchController {

    private final WosSearchService service;

    // Ör: /api/records/search?year=2025&q=network&subjectContent=Computer
    @GetMapping("/search")
    public List<RecDTO> search(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "doi", required = false) String doi,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "subjectContent", required = false) String subjectContent,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return service.search(year, q, doi, subject, subjectContent, page, size);
    }
}
