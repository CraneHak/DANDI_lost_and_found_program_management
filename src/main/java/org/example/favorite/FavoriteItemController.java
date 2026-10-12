package org.example.favorite;

import org.example.auth.FirebaseAuthenticationToken;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/users/favorites")
public class FavoriteItemController {

    private final FavoriteItemService favoriteItemService;

    public FavoriteItemController(FavoriteItemService favoriteItemService) {
        this.favoriteItemService = favoriteItemService;
    }

    @GetMapping
    public List<FavoriteResponse> getAll(Authentication authentication) {
        FirebaseAuthenticationToken token = requireFirebaseToken(authentication);
        return favoriteItemService.findAll(token.getUid()).stream()
                .map(FavoriteResponse::from)
                .toList();
    }

    @PutMapping("/{lostItemId}")
    public ResponseEntity<Void> add(Authentication authentication, @PathVariable Integer lostItemId) {
        FirebaseAuthenticationToken token = requireFirebaseToken(authentication);
        try {
            favoriteItemService.add(token.getUid(), lostItemId);
        } catch (FavoriteItemNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (DataIntegrityViolationException ignored) {
            // 동시에 두 번 저장 요청 — UNIQUE 제약으로 이미 1건 저장됨
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{lostItemId}")
    public ResponseEntity<Void> remove(Authentication authentication, @PathVariable Integer lostItemId) {
        FirebaseAuthenticationToken token = requireFirebaseToken(authentication);
        favoriteItemService.remove(token.getUid(), lostItemId);
        return ResponseEntity.noContent().build();
    }

    private FirebaseAuthenticationToken requireFirebaseToken(Authentication authentication) {
        if (!(authentication instanceof FirebaseAuthenticationToken token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return token;
    }

    public record FavoriteResponse(String lostItemId, String createdAt) {
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        static FavoriteResponse from(FavoriteItem f) {
            return new FavoriteResponse(
                    String.valueOf(f.getLostItemId()),
                    f.getCreatedAt() != null ? f.getCreatedAt().format(FMT) : null
            );
        }
    }
}
