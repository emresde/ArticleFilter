package com.example.wos.web;

import com.example.wos.dto.AscatypeCountDTO;
import com.example.wos.model.WosData;
import com.example.wos.service.WosRepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/repo")
@RequiredArgsConstructor
public class RepoController {

    private final WosRepoService service;

    // Subject'e göre kayıtları listele
    @GetMapping("/subject")
    public ResponseEntity<?> bySubject(@RequestParam(name = "subject", required = true) String subject) {
        List<WosData> res = service.getBySubjectContent(subject);
        return ResponseEntity.ok(res);
    }

    // Subject'e göre kaç kayıt var (toplam)
    @GetMapping("/subject/count")
    public ResponseEntity<?> countBySubject(@RequestParam(name = "subject", required = true) String subject) {
        long total = service.countBySubject(subject);
        return ResponseEntity.ok(Map.of("subject", subject, "total", total));
    }

    // Subject + ascatype birlikte (extended/traditional)
    @GetMapping("/subject/by-type")
    public List<WosData> bySubjectAndType(@RequestParam String subject,
                                          @RequestParam String ascatype) {
        return service.getBySubjectAndAscatype(subject, ascatype);
    }

    // Subject'e göre ascatype kırılımı (extended vs traditional)
    @GetMapping("/subject/count-by-type")
    public List<AscatypeCountDTO> countByType(@RequestParam(name = "subject", required = true) String subject) {
        return service.countBySubjectGroupedByAscatype(subject);
    }
}
