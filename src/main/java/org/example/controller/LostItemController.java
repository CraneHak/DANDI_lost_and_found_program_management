package org.example.controller;

import org.example.adminlog.AdminActionLogService;
import org.example.auth.FirebaseAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.example.entity.LostItem;
import org.example.service.LostItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/lost-items")
public class LostItemController {

    private final LostItemService lostItemService;
    private final AdminActionLogService adminActionLogService;

    public LostItemController(LostItemService lostItemService, AdminActionLogService adminActionLogService) {
        this.lostItemService = lostItemService;
        this.adminActionLogService = adminActionLogService;
    }

    @GetMapping
    public List<LostItemResponse> getAll() {
        return lostItemService.findAll().stream()
                .map(LostItemResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public LostItemResponse getById(@PathVariable Integer id) {
        return LostItemResponse.from(lostItemService.findById(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LostItemResponse> createJson(Authentication authentication, @RequestBody CreateLostItemRequest body) {
        try {
            LostItem saved = lostItemService.createFromRequest(body);
            logRegister(authentication, saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(LostItemResponse.from(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LostItemResponse> createMultipart(
            Authentication authentication,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String place,
            @RequestParam(required = false) String foundLocation,
            @RequestParam(required = false) String storage,
            @RequestParam(required = false) String storedLocation,
            @RequestParam(required = false) String memo,
            @RequestParam(required = false) String contact,
            @RequestParam(required = false) String lostAt,
            @RequestParam(required = false) String foundAt,
            @RequestParam(required = false) String acquiredAt,
            @RequestParam(required = false) String createdAt,
            @RequestParam(required = false) String registeredAt,
            @RequestParam(required = false) String storedDate,
            @RequestParam(required = false) String reportId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String photoUrl,
            @RequestParam(required = false) String mosaicImageUrl,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) throws IOException {
        try {
            LostItem saved = lostItemService.createFromMultipart(
                    itemName,
                    name,
                    category,
                    itemType,
                    location,
                    place,
                    foundLocation,
                    storage,
                    storedLocation,
                    memo,
                    contact,
                    lostAt,
                    foundAt,
                    acquiredAt,
                    createdAt,
                    registeredAt,
                    storedDate,
                    reportId,
                    status,
                    color,
                    imageUrl,
                    null,
                    photoUrl,
                    mosaicImageUrl,
                    image,
                    file
            );
            logRegister(authentication, saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(LostItemResponse.from(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}")
    public LostItemResponse patch(Authentication authentication, @PathVariable Integer id, @RequestBody UpdateLostItemRequest body) {
        LostItem saved = lostItemService.update(id, body);
        log(authentication, AdminActionLogService.UPDATE_ITEM, saved, "[" + name(saved) + "] 물품 정보 수정");
        return LostItemResponse.from(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Integer id) {
        try {
            LostItem target = lostItemService.findById(id);
            lostItemService.delete(id);
            log(authentication, AdminActionLogService.DELETE_ITEM, target, "[" + name(target) + "] 물품 삭제");
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    private void logRegister(Authentication authentication, LostItem saved) {
        log(authentication, AdminActionLogService.REGISTER, saved,
                "관리자 직접 등록: " + name(saved)
                        + (saved.getFoundLocation() != null ? " (" + saved.getFoundLocation() + ")" : ""));
    }

    // ponytail: 물품 저장과 별도 트랜잭션으로 남긴다. 저장 직후 기록이 실패해도 물품은 남는다.
    private void log(Authentication authentication, String action, LostItem item, String message) {
        if (!(authentication instanceof FirebaseAuthenticationToken token) || !token.isAdmin()) {
            return;
        }
        adminActionLogService.record(token.getUid(), token.getEmail(), action,
                item.getId() != null ? "lost-item-" + item.getId() : null, message);
    }

    private String name(LostItem item) {
        return item.getItemName() == null ? "물품" : item.getItemName();
    }
}
