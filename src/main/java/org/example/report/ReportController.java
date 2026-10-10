package org.example.report;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.auth.FirebaseAuthenticationToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReportResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateReportRequest body
    ) {
        try {
            Report report = new Report();
            report.setItemName(body.itemName());
            report.setCategory(body.category());
            report.setLostAt(body.lostAt());
            report.setLocation(body.location());
            report.setStorage(body.storage());
            report.setMemo(body.memo());
            report.setImage(body.resolvedImage());
            if (body.ownerEmail() != null && !body.ownerEmail().isBlank()) {
                report.setReporterEmail(body.ownerEmail().trim());
            }

            ReportActor actor = toActor(authentication);
            if (actor != null && report.getReporterEmail() == null) {
                report.setReporterEmail(actor.email());
            }

            Report saved = reportService.create(report, actor);
            return ResponseEntity.status(HttpStatus.CREATED).body(ReportResponse.from(saved));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportResponse> createMultipart(
            Authentication authentication,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String lostAt,
            @RequestParam(required = false) String foundAt,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String place,
            @RequestParam(required = false) String storage,
            @RequestParam(required = false) String memo,
            @RequestParam(required = false) String ownerEmail,
            @RequestParam(required = false) String reporterEmail,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String photoUrl,
            @RequestParam(required = false) String mosaicImageUrl,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        try {
            ReportActor actor = toActor(authentication);
            Report saved = reportService.createFromMultipart(
                    actor,
                    itemName,
                    name,
                    category,
                    lostAt,
                    foundAt,
                    location,
                    place,
                    storage,
                    memo,
                    ownerEmail,
                    reporterEmail,
                    imageUrl,
                    null,
                    photoUrl,
                    mosaicImageUrl,
                    image,
                    file
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(ReportResponse.from(saved));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 업로드에 실패했습니다.");
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest body
    ) {
        return applyStatusChange(authentication, id, body.status());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReportResponse> updateReport(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody UpdateReportDetailsRequest body
    ) {
        ReportActor actor = requireActor(authentication);
        try {
            Report saved = reportService.updateDetails(id, body, actor);
            return ResponseEntity.ok(ReportResponse.from(saved));
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        } catch (ReportNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReportResponse> replaceReport(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody UpdateReportDetailsRequest body
    ) {
        return updateReport(authentication, id, body);
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

    private ResponseEntity<Map<String, Object>> applyStatusChange(
            Authentication authentication,
            Long id,
            ReportStatus status
    ) {
        try {
            Report saved = reportService.updateStatus(id, status);
            return ResponseEntity.ok(Map.of(
                    "message", "상태 변경이 완료되었습니다.",
                    "id", saved.getId(),
                    "status", saved.getStatus().getValue()
            ));
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        } catch (ReportNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    private ReportActor toActor(Authentication authentication) {
        if (!(authentication instanceof FirebaseAuthenticationToken token)) {
            return null;
        }
        return new ReportActor(token.getUid(), token.getEmail(), token.isAdmin());
    }

    private ReportActor requireActor(Authentication authentication) {
        ReportActor actor = toActor(authentication);
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return actor;
    }


    public record CreateReportRequest(
            @NotBlank String itemName,
            String category,
            String lostAt,
            @NotBlank String location,
            String storage,
            String memo,
            String image,
            String imageUrl,
            String photoUrl,
            String ownerEmail
    ) {
        public String resolvedImage() {
            if (image != null && !image.isBlank()) return image.trim();
            if (imageUrl != null && !imageUrl.isBlank()) return imageUrl.trim();
            if (photoUrl != null && !photoUrl.isBlank()) return photoUrl.trim();
            return null;
        }
    }

    public record UpdateStatusRequest(
            @NotNull ReportStatus status
    ) {}

    public record ReportResponse(
            String id,
            String itemName,
            String name,
            String category,
            String lostAt,
            String foundAt,
            String location,
            String place,
            String storage,
            String memo,
            String imageUrl,
            String image,
            String photoUrl,
            ReportStatus status,
            String createdAt,
            String pickedUpAt,
            String ownerEmail,
            String ownerName
    ) {
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        static ReportResponse from(Report r) {
            String image = r.getImage();
            return new ReportResponse(
                    String.valueOf(r.getId()),
                    r.getItemName(),
                    r.getItemName(),
                    r.getCategory(),
                    r.getLostAt(),
                    r.getLostAt(),
                    r.getLocation(),
                    r.getLocation(),
                    r.getStorage(),
                    r.getMemo(),
                    image,
                    image,
                    image,
                    r.getStatus(),
                    r.getCreatedAt() != null ? r.getCreatedAt().format(FMT) : null,
                    r.getPickedUpAt() != null ? r.getPickedUpAt().format(FMT) : null,
                    r.getReporterEmail(),
                    null
            );
        }
    }
}
