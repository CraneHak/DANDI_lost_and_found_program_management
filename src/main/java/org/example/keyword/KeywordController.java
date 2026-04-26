package org.example.keyword;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users/keywords")
public class KeywordController {

    private final KeywordService keywordService;

    public KeywordController(KeywordService keywordService) {
        this.keywordService = keywordService;
    }

    @GetMapping
    public List<KeywordResponse> getAll() {
        return keywordService.findAll().stream()
                .map(KeywordResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<KeywordResponse> add(@Valid @RequestBody AddKeywordRequest body) {
        Keyword saved = keywordService.add(body.keyword());
        return ResponseEntity.status(HttpStatus.CREATED).body(KeywordResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            keywordService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (KeywordNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    // ── Request / Response records ──────────────────────────────────────────

    public record AddKeywordRequest(
            @NotBlank String keyword
    ) {}

    public record KeywordResponse(
            String id,
            String keyword,
            String createdAt
    ) {
        static KeywordResponse from(Keyword k) {
            return new KeywordResponse(
                    String.valueOf(k.getId()),
                    k.getKeyword(),
                    k.getCreatedAt() != null ? k.getCreatedAt().toString() : null
            );
        }
    }
}
