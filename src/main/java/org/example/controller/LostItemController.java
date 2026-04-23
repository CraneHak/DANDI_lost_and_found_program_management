package org.example.controller;

import org.example.entity.LostItem;
import org.example.service.LostItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public List<LostItem> getAll() {
        return lostItemService.findAll();
    }

    @GetMapping("/{id}")
    public LostItem getById(@PathVariable Integer id) {
        return lostItemService.findById(id);
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<LostItem> create(
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
        if (storedDate != null) item.setStoredDate(LocalDate.parse(storedDate));
        item.setContact(contact);
        item.setColor(color);
        item.setItemType(itemType);

        return ResponseEntity.ok(lostItemService.save(item, image));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        lostItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
