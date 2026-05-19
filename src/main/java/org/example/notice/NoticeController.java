package org.example.notice;

import org.example.auth.FirebaseAuthenticationToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public List<NoticeResponse> getAll(Authentication authentication) {
        FirebaseAuthenticationToken token = requireFirebaseToken(authentication);
        return noticeService.findAll(token.getUid()).stream()
                .map(NoticeResponse::from)
                .toList();
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markRead(Authentication authentication, @PathVariable Long id) {
        FirebaseAuthenticationToken token = requireFirebaseToken(authentication);
        try {
            noticeService.markRead(token.getUid(), id);
            return ResponseEntity.ok(Map.of("message", "read updated"));
        } catch (NoticeNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        FirebaseAuthenticationToken token = requireFirebaseToken(authentication);
        try {
            noticeService.delete(token.getUid(), id);
            return ResponseEntity.noContent().build();
        } catch (NoticeNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    private FirebaseAuthenticationToken requireFirebaseToken(Authentication authentication) {
        if (!(authentication instanceof FirebaseAuthenticationToken token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return token;
    }

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
