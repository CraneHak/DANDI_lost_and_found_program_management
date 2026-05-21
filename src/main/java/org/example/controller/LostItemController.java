package org.example.controller;

import org.example.entity.LostItem;
import org.example.service.LostItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/lost-items")
public class LostItemController {

    private final LostItemService lostItemService;

    public LostItemController(LostItemService lostItemService) {
        this.lostItemService = lostItemService;
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
    public ResponseEntity<LostItemResponse> createJson(@RequestBody CreateLostItemRequest body) {
        try {
            LostItem saved = lostItemService.createFromRequest(body);
            return ResponseEntity.status(HttpStatus.CREATED).body(LostItemResponse.from(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LostItemResponse> createMultipart(
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String foundLocation,
            @RequestParam(required = false) String storedLocation,
            @RequestParam(required = false) String storedDate,
            @RequestParam(required = false) String contact,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) MultipartFile image
    ) throws IOException {
        LostItem item = new LostItem();
        item.setItemName(itemName);
        item.setFoundLocation(foundLocation);
        item.setStoredLocation(storedLocation);
        if (storedDate != null) {
            item.setStoredDate(LocalDate.parse(storedDate));
        }
        item.setContact(contact);
        item.setColor(color);
        item.setItemType(itemType);

        LostItem saved = lostItemService.save(item, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(LostItemResponse.from(saved));
    }

    @PatchMapping("/{id}")
    public LostItemResponse patch(@PathVariable Integer id, @RequestBody UpdateLostItemRequest body) {
        return LostItemResponse.from(lostItemService.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        try {
            lostItemService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }
}
