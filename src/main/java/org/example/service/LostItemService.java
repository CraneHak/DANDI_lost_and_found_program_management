package org.example.service;

import org.example.common.ImageUrlService;
import org.example.controller.CreateLostItemRequest;
import org.example.controller.UpdateLostItemRequest;
import org.example.entity.ItemStatus;
import org.example.entity.LostItem;
import org.example.pickup.CollectionLogRepository;
import org.example.pickup.PickupPassRepository;
import org.example.keyword.KeywordAlertService;
import org.example.report.Report;
import org.example.report.ReportRepository;
import org.example.repository.LostItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class LostItemService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final LostItemRepository repository;
    private final S3Service s3Service;
    private final ImageUrlService imageUrlService;
    private final CollectionLogRepository collectionLogRepository;
    private final PickupPassRepository pickupPassRepository;
    private final ReportRepository reportRepository;
    private final KeywordAlertService keywordAlertService;

    public LostItemService(
            LostItemRepository repository,
            S3Service s3Service,
            ImageUrlService imageUrlService,
            CollectionLogRepository collectionLogRepository,
            PickupPassRepository pickupPassRepository,
            ReportRepository reportRepository,
            KeywordAlertService keywordAlertService
    ) {
        this.repository = repository;
        this.s3Service = s3Service;
        this.imageUrlService = imageUrlService;
        this.collectionLogRepository = collectionLogRepository;
        this.pickupPassRepository = pickupPassRepository;
        this.reportRepository = reportRepository;
        this.keywordAlertService = keywordAlertService;
    }

    public List<LostItem> findAll() {
        return repository.findAll();
    }

    public LostItem findById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "분실물을 찾을 수 없습니다. id=" + id));
    }

    public LostItem save(LostItem lostItem, MultipartFile image) throws IOException {
        if (image != null && !image.isEmpty()) {
            lostItem.setImageUrl(s3Service.upload(image));
        }
        return repository.save(lostItem);
    }

    /**
     * 프론트 관리자 등록 multipart — JSON {@link CreateLostItemRequest}와 동일한 필드 별칭 수용.
     */
    @Transactional
    public LostItem createFromMultipart(
            String itemName,
            String name,
            String category,
            String itemType,
            String location,
            String place,
            String foundLocation,
            String storage,
            String storedLocation,
            String memo,
            String contact,
            String lostAt,
            String foundAt,
            String acquiredAt,
            String createdAt,
            String registeredAt,
            String storedDate,
            String reportId,
            String status,
            String color,
            String imageUrl,
            String imageField,
            String photoUrl,
            String mosaicImageUrl,
            MultipartFile imagePart,
            MultipartFile filePart
    ) throws IOException {
        String resolvedName = firstNonBlank(itemName, name);
        if (resolvedName == null) {
            throw new IllegalArgumentException("name or itemName is required.");
        }

        LostItem item = new LostItem();
        Long linkedReportId = parseReportId(reportId);
        if (linkedReportId != null) {
            if (!reportRepository.existsById(linkedReportId)) {
                throw new IllegalArgumentException("report not found: id=" + linkedReportId);
            }
            item = repository.findByReportId(linkedReportId).orElseGet(LostItem::new);
            item.setReportId(linkedReportId);
        }
        boolean wasPublic = isPublic(item);

        item.setItemName(resolvedName);
        String type = resolveItemType(itemType, category);
        if (type != null) {
            item.setItemType(type);
        }
        String placeValue = firstNonBlank(location, place, foundLocation);
        if (placeValue != null) {
            item.setFoundLocation(placeValue);
            item.setLostLocation(placeValue);
        }
        String storageValue = firstNonBlank(storage, storedLocation);
        if (storageValue != null) {
            item.setStoredLocation(storageValue);
        }
        String contactValue = firstNonBlank(memo, contact);
        if (contactValue != null) {
            item.setContact(contactValue);
        }
        if (color != null) {
            item.setColor(color.trim());
        }
        parseStoredDate(firstNonBlank(lostAt, foundAt, acquiredAt, createdAt, registeredAt, storedDate))
                .ifPresent(item::setStoredDate);

        String image = resolveLostItemImage(imagePart, filePart, imageUrl, imageField, photoUrl, mosaicImageUrl);
        if (image != null) {
            item.setImageUrl(image);
        }

        // 엔티티 기본값이 STORED라 null 체크만 하면 published가 반영되지 않음
        // 이미 공개·수령 단계인 물품(신고 승인으로 생성된 행 등)은 상태를 되돌리지 않는다.
        if (!wasPublic) {
            item.setStatus(resolveItemStatus(status));
        }

        return persist(item, wasPublic);
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
        boolean wasPublic = isPublic(item);
        applyRequest(item, request);
        if (!wasPublic) {
            item.setStatus(ItemStatus.ACQUIRED);
        }
        return persist(item, wasPublic);
    }

    @Transactional
    public LostItem upsertFromReport(Report report) {
        LostItem item = repository.findByReportId(report.getId()).orElseGet(LostItem::new);
        boolean wasPublic = isPublic(item);
        item.setReportId(report.getId());
        item.setItemName(report.getItemName());
        item.setItemType(resolveItemType(null, report.getCategory()));
        item.setFoundLocation(report.getLocation());
        item.setLostLocation(report.getLocation());
        item.setStoredLocation(report.getStorage());
        item.setContact(report.getMemo());
        if (!wasPublic) {
            item.setStatus(ItemStatus.ACQUIRED);
        }
        if (report.getImage() != null && !report.getImage().isBlank()) {
            item.setImageUrl(imageUrlService.normalizeForStorage(report.getImage()));
        }
        parseStoredDate(report.getLostAt()).ifPresent(item::setStoredDate);
        return persist(item, wasPublic);
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
        String patchType = resolveItemType(request.itemType(), request.category());
        if (patchType != null) {
            item.setItemType(patchType);
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

    /** 키워드 알림은 처음 공개될 때 한 번만 보낸다 (수정·재승인·중복 등록 요청에는 보내지 않음). */
    private LostItem persist(LostItem item, boolean wasPublic) {
        LostItem saved = repository.save(item);
        if (!wasPublic) {
            keywordAlertService.notifyMatchingUsers(saved);
        }
        return saved;
    }

    private boolean isPublic(LostItem item) {
        if (item.getId() == null) {
            return false;
        }
        ItemStatus status = item.getStatus();
        return status == ItemStatus.ACQUIRED
                || status == ItemStatus.PENDING_RECEIPT
                || status == ItemStatus.RECEIVED;
    }

    private String resolveItemType(String itemType, String category) {
        String raw = firstNonBlank(itemType, category);
        if (raw == null) {
            return null;
        }
        if (!raw.contains(",")) {
            return raw;
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                unique.add(trimmed);
            }
        }
        if (unique.isEmpty()) {
            return null;
        }
        return String.join(",", unique);
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
        String type = resolveItemType(request.itemType(), request.category());
        if (type != null) {
            item.setItemType(type);
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

    private java.util.Optional<LocalDateTime> parseStoredDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return java.util.Optional.empty();
        }
        String normalized = raw.trim().replace(" ", "T");
        try {
            return java.util.Optional.of(LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (Exception ignored) {
            // fall through
        }
        try {
            // 프론트는 UTC ISO(예: 2026-10-10T09:30:00.000Z)를 보낸다 — 한국 시각으로 바꿔 시·분을 유지
            return java.util.Optional.of(OffsetDateTime.parse(normalized)
                    .atZoneSameInstant(SEOUL)
                    .toLocalDateTime());
        } catch (Exception ignored) {
            // fall through
        }
        try {
            if (normalized.length() >= 10) {
                return java.util.Optional.of(
                        LocalDate.parse(normalized.substring(0, 10)).atStartOfDay()
                );
            }
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.empty();
    }

    private Long parseReportId(String reportId) {
        if (reportId == null || reportId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(reportId.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private ItemStatus resolveItemStatus(String status) {
        if (status == null || status.isBlank()) {
            return ItemStatus.ACQUIRED;
        }
        String normalized = status.trim().toLowerCase();
        if ("published".equals(normalized) || "acquired".equals(normalized)) {
            return ItemStatus.ACQUIRED;
        }
        if ("stored".equals(normalized)) {
            return ItemStatus.STORED;
        }
        try {
            return ItemStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ItemStatus.ACQUIRED;
        }
    }

    private String resolveLostItemImage(
            MultipartFile imagePart,
            MultipartFile filePart,
            String imageUrl,
            String imageField,
            String photoUrl,
            String mosaicImageUrl
    ) throws IOException {
        if (imagePart != null && !imagePart.isEmpty()) {
            return s3Service.upload(imagePart);
        }
        if (filePart != null && !filePart.isEmpty()) {
            return s3Service.upload(filePart);
        }
        String urlCandidate = firstNonBlank(imageUrl, mosaicImageUrl, photoUrl, imageField);
        if (urlCandidate != null) {
            return imageUrlService.normalizeForStorage(urlCandidate);
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }
}
