package com.example.wos.web;

import com.example.wos.dto.CountDTO;
import com.example.wos.dto.YearCountDTO;
import com.example.wos.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService stats;

    // Ör: /api/stats/subject-content?subjectContent=Computer
    @GetMapping("/subject-content")
    public CountDTO countBySubjectContent(@RequestParam(value = "subjectContent", required = false) String subjectContent) {
        return stats.countBySubjectContent(subjectContent);
    }
}
