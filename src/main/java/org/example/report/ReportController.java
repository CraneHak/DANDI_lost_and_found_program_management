package org.example.report;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public List<ReportResponse> getAll() {
        return reportService.findAll().stream()
                .map(ReportResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ReportResponse> create(@Valid @RequestBody CreateReportRequest body) {
        try {
            Report report = new Report();
            report.setItemName(body.itemName());
            report.setCategory(body.category());
            report.setLostAt(body.lostAt());
            report.setLocation(body.location());
            report.setMemo(body.memo());

            Report saved = reportService.create(report);
            return ResponseEntity.status(HttpStatus.CREATED).body(ReportResponse.from(saved));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest body
    ) {
        try {
            reportService.updateStatus(id, body.status());
            return ResponseEntity.ok(Map.of("message", "상태 변경이 완료되었습니다."));
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        } catch (ReportNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            reportService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        } catch (ReportNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    // ── Request / Response records ──────────────────────────────────────────

    public record CreateReportRequest(
            @NotBlank String itemName,
            String category,
            String lostAt,
            @NotBlank String location,
            String memo
    ) {}

    public record UpdateStatusRequest(
            @NotNull ReportStatus status
    ) {}

    public record ReportResponse(
            String id,
            String itemName,
            String category,
            String lostAt,
            String location,
            String memo,
            ReportStatus status,
            String createdAt,
            String pickedUpAt
    ) {
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        static ReportResponse from(Report r) {
            return new ReportResponse(
                    String.valueOf(r.getId()),
                    r.getItemName(),
                    r.getCategory(),
                    r.getLostAt(),
                    r.getLocation(),
                    r.getMemo(),
                    r.getStatus(),
                    r.getCreatedAt() != null ? r.getCreatedAt().format(FMT) : null,
                    r.getPickedUpAt() != null ? r.getPickedUpAt().format(FMT) : null
            );
        }
    }
}
