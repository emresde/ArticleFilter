package com.example.wos.web;

import com.example.wos.dto.CountDTO;
import com.example.wos.dto.SubjectCountDTO;
import com.example.wos.dto.SubjectTypeCountDTO;
import com.example.wos.dto.YearCountDTO;
import com.example.wos.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService stats;

    /* ------------------------ Helpers ------------------------ */
    private String csvEscape(String s) {
        if (s == null) return "";
        // basit kaçış: çift tırnak içine al, içteki tırnakları kaçır
        String escaped = s.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    /* ------------------------ Endpoints ------------------------ */

    // Tek subjectContent için toplam
    @GetMapping("/subject-content")
    public ResponseEntity<?> countBySubjectContent(
            @RequestParam(value = "subjectContent", required = false) String subjectContent,
            @RequestParam(value = "format", required = false) String format
    ) {
        CountDTO out = stats.countBySubjectContent(subjectContent);
        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder();
            sb.append("total\n").append(out.getTotal()).append("\n");
            return ResponseEntity.ok().header("Content-Type", "text/csv").body(sb.toString());
        }
        return ResponseEntity.ok(out);
    }

    // Tüm subject'ler ve sayıları (opsiyonel ascatype filtresi)
    @GetMapping("/subjects")
    public ResponseEntity<?> allSubjects(
            @RequestParam(value = "ascatype", required = false) String ascatype,
            @RequestParam(value = "format", required = false) String format
    ) {
        List<SubjectCountDTO> data = stats.allSubjectsCount(ascatype);
        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder();
            sb.append("subject,count\n");
            for (SubjectCountDTO d : data) {
                sb.append(csvEscape(d.getSubject())).append(",")
                  .append(d.getCount()).append("\n");
            }
            return ResponseEntity.ok().header("Content-Type", "text/csv").body(sb.toString());
        }
        return ResponseEntity.ok(data);
    }

    // Yıllara göre dağılım (opsiyonel subjectContent filtresi)
    @GetMapping("/by-year")
    public ResponseEntity<?> byYear(
            @RequestParam(value = "subjectContent", required = false) String subjectContent,
            @RequestParam(value = "format", required = false) String format
    ) {
        List<YearCountDTO> data = stats.histogramByYear(subjectContent);
        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder();
            sb.append("year,count\n");
            for (YearCountDTO d : data) {
                sb.append(d.getYear() == null ? "" : d.getYear()).append(",")
                  .append(d.getCount()).append("\n");
            }
            return ResponseEntity.ok().header("Content-Type", "text/csv").body(sb.toString());
        }
        return ResponseEntity.ok(data);
    }

    // ascatype toplamları (extended vs traditional)
    // Not: Service, SubjectCountDTO(subject, count) döndürüyor; burada subject = ascatype
    @GetMapping("/ascatype")
    public ResponseEntity<?> totalsByAscatype(
            @RequestParam(value = "format", required = false) String format
    ) {
        List<SubjectCountDTO> data = stats.totalsByAscatype();
        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder();
            sb.append("ascatype,count\n");
            for (SubjectCountDTO d : data) {
                sb.append(csvEscape(d.getSubject())).append(",")
                  .append(d.getCount()).append("\n");
            }
            return ResponseEntity.ok().header("Content-Type", "text/csv").body(sb.toString());
        }
        return ResponseEntity.ok(data);
    }

    // Verilen subjectContent için ascatype kırılımı
    // Not: Service, SubjectCountDTO(subject, count) döndürüyor; burada subject = ascatype
    @GetMapping("/subject/count-by-type")
    public ResponseEntity<?> subjectAscatypeBreakdown(
            @RequestParam(name = "subject", required = true) String subjectContent,
            @RequestParam(value = "format", required = false) String format
    ) {
        List<SubjectCountDTO> data = stats.subjectAscatypeBreakdown(subjectContent);
        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder();
            sb.append("ascatype,count\n");
            for (SubjectCountDTO d : data) {
                sb.append(csvEscape(d.getSubject())).append(",")
                  .append(d.getCount()).append("\n");
            }
            return ResponseEntity.ok().header("Content-Type", "text/csv").body(sb.toString());
        }
        return ResponseEntity.ok(data);
    }

    // subject+ascatype kırılımı (yazar sayısı filtresiyle)
    @GetMapping("/authors/by-subject")
    public ResponseEntity<?> subjectsForAuthorCount(
            @RequestParam(value = "ascatype", required = false) String ascatype,
            @RequestParam(value = "subjectContent", required = false) String subjectContent,
            @RequestParam(value = "lt", required = false) Integer lt,
            @RequestParam(value = "gte", required = false) Integer gte,
            @RequestParam(value = "exact", required = false, defaultValue = "false") boolean exact,
            @RequestParam(value = "format", required = false) String format
    ) {
        List<SubjectTypeCountDTO> data = stats.subjectDistributionForAuthorCount(ascatype, subjectContent, lt, gte, exact);

        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder();
            sb.append("subject,ascatype,count\n");
            for (SubjectTypeCountDTO d : data) {
                sb.append(csvEscape(d.getSubject())).append(",")
                  .append(csvEscape(d.getAscatype())).append(",")
                  .append(d.getCount()).append("\n");
            }
            return ResponseEntity.ok()
                    .header("Content-Type", "text/csv")
                    .body(sb.toString());
        } else {
            return ResponseEntity.ok(data); // default JSON
        }
    }

    // yazar sayısına göre toplam (opsiyonel ascatype + subjectContent)
    @GetMapping("/authors/count")
    public ResponseEntity<?> countByAuthorCount(
            @RequestParam(value = "ascatype", required = false) String ascatype,
            @RequestParam(value = "subjectContent", required = false) String subjectContent,
            @RequestParam(value = "lt", required = false) Integer lt,
            @RequestParam(value = "gte", required = false) Integer gte,
            @RequestParam(value = "format", required = false) String format
    ) {
        CountDTO out = stats.countArticlesByAuthorCount(ascatype, subjectContent, lt, gte);
        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder();
            sb.append("total\n").append(out.getTotal()).append("\n");
            return ResponseEntity.ok().header("Content-Type", "text/csv").body(sb.toString());
        }
        return ResponseEntity.ok(out);
    }
}
