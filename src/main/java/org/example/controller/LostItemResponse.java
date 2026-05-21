package org.example.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.entity.ItemStatus;
import org.example.entity.LostItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LostItemResponse(
        Integer id,
        Long reportId,
        String name,
        String itemName,
        String category,
        String place,
        String location,
        String foundAt,
        String lostAt,
        String storage,
        String imageUrl,
        String image,
        String photoUrl,
        String memo,
        ItemStatus status,
        String createdAt,
        String updatedAt,
        String storedDate
) {
    private static final DateTimeFormatter API_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static LostItemResponse from(LostItem item) {
        String category = item.getItemType();
        if (category == null && item.getCategory() != null) {
            category = item.getCategory().getName();
        }
        String place = item.getFoundLocation() != null ? item.getFoundLocation() : item.getLostLocation();
        String foundAt = formatDateTime(item.getStoredDate());
        String imageUrl = item.getImageUrl();
        return new LostItemResponse(
                item.getId(),
                item.getReportId(),
                item.getItemName(),
                item.getItemName(),
                category,
                place,
                place,
                foundAt,
                foundAt,
                item.getStoredLocation(),
                imageUrl,
                imageUrl,
                imageUrl,
                item.getContact() != null && !item.getContact().isBlank() ? item.getContact() : null,
                item.getStatus(),
                formatDateTime(item.getCreatedAt()),
                formatDateTime(item.getUpdatedAt()),
                foundAt
        );
    }

    static String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(API_DATE_TIME);
    }
}
