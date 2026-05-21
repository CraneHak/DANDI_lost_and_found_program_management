package org.example.service;

import org.example.common.ImageUrlService;
import org.example.controller.CreateLostItemRequest;
import org.example.controller.UpdateLostItemRequest;
import org.example.entity.ItemStatus;
import org.example.entity.LostItem;
import org.example.pickup.CollectionLogRepository;
import org.example.pickup.PickupPassRepository;
import org.example.report.Report;
import org.example.report.ReportRepository;
import org.example.repository.LostItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class LostItemService {

    private final LostItemRepository repository;
    private final S3Service s3Service;
    private final ImageUrlService imageUrlService;
    private final CollectionLogRepository collectionLogRepository;
    private final PickupPassRepository pickupPassRepository;
    private final ReportRepository reportRepository;

    public LostItemService(
            LostItemRepository repository,
            S3Service s3Service,
            ImageUrlService imageUrlService,
            CollectionLogRepository collectionLogRepository,
            PickupPassRepository pickupPassRepository,
            ReportRepository reportRepository
    ) {
        this.repository = repository;
        this.s3Service = s3Service;
        this.imageUrlService = imageUrlService;
        this.collectionLogRepository = collectionLogRepository;
        this.pickupPassRepository = pickupPassRepository;
        this.reportRepository = reportRepository;
    }

    public List<LostItem> findAll() {
        return repository.findAll();
    }

    public LostItem findById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("분실물을 찾을 수 없습니다. id=" + id));
    }

    public LostItem save(LostItem lostItem, MultipartFile image) throws IOException {
        if (image != null && !image.isEmpty()) {
            lostItem.setImageUrl(s3Service.upload(image));
        }
        return repository.save(lostItem);
    }

    @Transactional
    public LostItem createFromRequest(CreateLostItemRequest request) {
        String name = request.resolvedName();
        if (name == null) {
            throw new IllegalArgumentException("name or itemName is required.");
        }
        LostItem item = request.reportId() != null
                ? repository.findByReportId(request.reportId()).orElseGet(LostItem::new)
                : new LostItem();
        if (request.reportId() != null) {
            item.setReportId(request.reportId());
        }
        applyRequest(item, request);
        if (item.getStatus() == null) {
            item.setStatus(ItemStatus.ACQUIRED);
        }
        return repository.save(item);
    }

    @Transactional
    public LostItem upsertFromReport(Report report) {
        LostItem item = repository.findByReportId(report.getId()).orElseGet(LostItem::new);
        item.setReportId(report.getId());
        item.setItemName(report.getItemName());
        item.setItemType(report.getCategory());
        item.setFoundLocation(report.getLocation());
        item.setLostLocation(report.getLocation());
        item.setStoredLocation(report.getStorage());
        item.setContact(report.getMemo());
        item.setStatus(ItemStatus.ACQUIRED);
        if (report.getImage() != null && !report.getImage().isBlank()) {
            item.setImageUrl(imageUrlService.normalizeForStorage(report.getImage()));
        }
        parseStoredDate(report.getLostAt()).ifPresent(item::setStoredDate);
        return repository.save(item);
    }

    @Transactional
    public LostItem update(Integer id, UpdateLostItemRequest request) {
        LostItem item = findById(id);
        if (request.name() != null || request.itemName() != null) {
            String name = request.name() != null && !request.name().isBlank()
                    ? request.name().trim()
                    : request.itemName() != null ? request.itemName().trim() : null;
            if (name != null) {
                item.setItemName(name);
            }
        }
        if (request.category() != null) {
            item.setItemType(request.category().trim());
        }
        if (request.itemType() != null) {
            item.setItemType(request.itemType().trim());
        }
        String place = request.place() != null && !request.place().isBlank()
                ? request.place().trim()
                : request.location() != null ? request.location().trim() : null;
        if (place != null) {
            item.setFoundLocation(place);
            item.setLostLocation(place);
        }
        if (request.storage() != null) {
            item.setStoredLocation(request.storage().trim());
        }
        if (request.memo() != null) {
            item.setContact(request.memo().trim());
        }
        if (request.foundAt() != null) {
            parseStoredDate(request.foundAt()).ifPresent(item::setStoredDate);
        }
        if (request.image() != null && !request.image().isBlank()) {
            item.setImageUrl(imageUrlService.normalizeForStorage(request.image()));
        }
        return repository.save(item);
    }

    @Transactional
    public void delete(Integer id) {
        LostItem item = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("분실물을 찾을 수 없습니다. id=" + id));
        deleteCascade(item, true);
    }

    /** 연결 분실물만 제거 (신고 삭제 API에서 report 행은 ReportService가 삭제) */
    @Transactional
    public void deleteByReportId(Long reportId) {
        repository.findByReportId(reportId).ifPresent(item -> deleteCascade(item, false));
    }

    /**
     * 수령 로그·수령증 → 분실물 → (선택) 연결 신고(report) 순으로 제거.
     */
    private void deleteCascade(LostItem item, boolean deleteLinkedReport) {
        Integer lostItemId = item.getId();
        Long reportId = item.getReportId();

        collectionLogRepository.deleteByLostItemId(lostItemId);
        pickupPassRepository.deleteByLostItem_Id(lostItemId);
        s3Service.deleteByUrlIfPresent(item.getImageUrl());

        repository.delete(item);

        if (deleteLinkedReport && reportId != null && reportRepository.existsById(reportId)) {
            reportRepository.deleteById(reportId);
        }
    }

    private void applyRequest(LostItem item, CreateLostItemRequest request) {
        item.setItemName(request.resolvedName());
        if (request.category() != null) {
            item.setItemType(request.category().trim());
        }
        if (request.itemType() != null) {
            item.setItemType(request.itemType().trim());
        }
        String place = request.resolvedPlace();
        if (place != null) {
            item.setFoundLocation(place);
            item.setLostLocation(place);
        }
        if (request.storage() != null) {
            item.setStoredLocation(request.storage().trim());
        }
        if (request.memo() != null) {
            item.setContact(request.memo().trim());
        }
        if (request.contact() != null) {
            item.setContact(request.contact().trim());
        }
        String image = request.resolvedImage();
        if (image != null) {
            item.setImageUrl(imageUrlService.normalizeForStorage(image));
        }
        parseStoredDate(request.resolvedFoundAt()).ifPresent(item::setStoredDate);
    }

    private java.util.Optional<LocalDate> parseStoredDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return java.util.Optional.empty();
        }
        String normalized = raw.trim().replace(" ", "T");
        try {
            if (normalized.length() >= 10) {
                return java.util.Optional.of(LocalDate.parse(normalized.substring(0, 10)));
            }
        } catch (Exception ignored) {
            // fall through
        }
        try {
            return java.util.Optional.of(
                    LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate()
            );
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }
}
