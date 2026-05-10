package org.example.notice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public List<NoticeResponse> getAll() {
        return noticeService.findAll().stream()
                .map(NoticeResponse::from)
                .toList();
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markRead(@PathVariable Long id) {
        try {
            noticeService.markRead(id);
            return ResponseEntity.ok(Map.of("message", "읽음 처리되었습니다."));
        } catch (NoticeNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            noticeService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NoticeNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    // ── Response record ──────────────────────────────────────────────────────

    public record NoticeResponse(
            String id,
            String title,
            String message,
            boolean read,
            String createdAt
    ) {
        static NoticeResponse from(Notice n) {
            return new NoticeResponse(
                    String.valueOf(n.getId()),
                    n.getTitle(),
                    n.getMessage(),
                    n.isRead(),
                    n.getCreatedAt() != null ? n.getCreatedAt().toString() : null
            );
        }
    }
}
