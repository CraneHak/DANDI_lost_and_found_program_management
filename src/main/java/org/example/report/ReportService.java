package org.example.report;

import org.example.common.ImageUrlService;
import org.example.entity.ItemStatus;
import org.example.entity.LostItem;
import org.example.notice.NoticeService;
import org.example.service.LostItemService;
import org.example.service.S3Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final NoticeService noticeService;
    private final LostItemService lostItemService;
    private final S3Service s3Service;
    private final ImageUrlService imageUrlService;

    public ReportService(
            ReportRepository reportRepository,
            NoticeService noticeService,
            LostItemService lostItemService,
            S3Service s3Service,
            ImageUrlService imageUrlService
    ) {
        this.reportRepository = reportRepository;
        this.noticeService = noticeService;
        this.lostItemService = lostItemService;
        this.s3Service = s3Service;
        this.imageUrlService = imageUrlService;
    }

    public List<Report> findAll() {
        return reportRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Report create(Report report, ReportActor actor) {
        normalizeReportFields(report);
        report.setStatus(ReportStatus.PENDING);
        if (actor != null) {
            report.setReporterUid(actor.uid());
            if (actor.email() != null) {
                report.setReporterEmail(actor.email());
            }
        }
        Report saved = reportRepository.save(report);
        if (actor != null) {
            noticeService.create(
                    actor.uid(),
                    "분실물 신고 접수",
                    "분실물 신고가 접수되었습니다: " + safe(saved.getItemName())
            );
        }
        return saved;
    }

    /**
     * 프론트 신고 등록 multipart (image / file 필드 + 텍스트 폼 필드).
     */
    @Transactional
    public Report createFromMultipart(
            ReportActor actor,
            String itemName,
            String name,
            String category,
            String lostAt,
            String foundAt,
            String location,
            String place,
            String storage,
            String memo,
            String ownerEmail,
            String reporterEmail,
            String imageUrl,
            String imageField,
            String photoUrl,
            String mosaicImageUrl,
            MultipartFile imagePart,
            MultipartFile filePart
    ) throws IOException {
        Report report = new Report();
        report.setItemName(firstNonBlank(itemName, name));
        report.setCategory(trimToNull(category));
        report.setLostAt(firstNonBlank(lostAt, foundAt));
        report.setLocation(firstNonBlank(location, place));
        report.setStorage(trimToNull(storage));
        report.setMemo(memo);
        report.setImage(resolveReportImage(imagePart, filePart, imageUrl, imageField, photoUrl, mosaicImageUrl));

        String email = firstNonBlank(ownerEmail, reporterEmail);
        if (email != null) {
            report.setReporterEmail(email);
        }

        return create(report, actor);
    }

    /**
     * 검수 대기 신고의 내용만 수정 (status는 pending 유지).
     */
    @Transactional
    public Report updateDetails(Long id, UpdateReportDetailsRequest body, ReportActor actor) {
        if (actor == null || !actor.admin()) {
            throw new IllegalStateException("관리자만 신고 내용을 수정할 수 있습니다.");
        }
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ReportNotFoundException(id));
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new IllegalStateException("검수 대기 중인 신고만 수정할 수 있습니다.");
        }

        String itemName = body.resolvedItemName();
        if (itemName != null) {
            report.setItemName(itemName);
        }
        String category = body.resolvedCategory();
        if (category != null) {
            report.setCategory(category);
        }
        String location = body.resolvedLocation();
        if (location != null) {
            report.setLocation(location);
        }
        if (body.storage() != null) {
            report.setStorage(trimToNull(body.storage()));
        }
        if (body.memo() != null) {
            report.setMemo(trimToNull(body.memo()));
        }
        String lostAt = body.resolvedLostAt();
        if (lostAt != null) {
            report.setLostAt(lostAt);
        }
        String image = body.resolvedImage();
        if (image != null) {
            report.setImage(imageUrlService.normalizeForStorage(image));
        }

        normalizeReportFields(report);
        return reportRepository.save(report);
    }

    @Transactional
    public Report updateStatus(Long id, ReportStatus newStatus) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ReportNotFoundException(id));
        validateStatusTransition(report.getStatus(), newStatus);
        report.setStatus(newStatus);
        if (newStatus == ReportStatus.PICKED_UP) {
            report.setPickedUpAt(LocalDateTime.now());
        } else if (newStatus != ReportStatus.PICKED_UP) {
            report.setPickedUpAt(null);
        }
        Report saved = reportRepository.save(report);

        // 처리 결과는 처리자(관리자)가 아니라 신고자에게 보낸다.
        String reporterUid = saved.getReporterUid();
        if (reporterUid != null && !reporterUid.isBlank()) {
            String title = switch (newStatus) {
                case RESOLVED -> "습득 완료";
                case PICKED_UP -> "최종 수령 완료";
                case UNAVAILABLE -> "습득 불가";
                default -> "신고 상태 변경";
            };
            String message = switch (newStatus) {
                case RESOLVED -> "습득이 확인되어 홈에 공개되었습니다: " + safe(saved.getItemName());
                case UNAVAILABLE -> "아직 습득되지 않았습니다: " + safe(saved.getItemName());
                case PICKED_UP -> "물품 수령이 완료되었습니다: " + safe(saved.getItemName());
                default -> "신고 상태가 변경되었습니다: " + safe(saved.getItemName());
            };
            noticeService.create(reporterUid, title, message);
        }

        if (newStatus == ReportStatus.RESOLVED) {
            lostItemService.upsertFromReport(saved);
        }

        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ReportNotFoundException(id));
        if (report.getStatus() == ReportStatus.PICKED_UP) {
            throw new IllegalStateException("최종 수령 완료된 신고는 삭제할 수 없습니다.");
        }
        lostItemService.deleteByReportId(id);
        reportRepository.deleteById(id);
    }

    private void normalizeReportFields(Report report) {
        report.setItemName(trimToNull(report.getItemName()));
        report.setCategory(trimToNull(report.getCategory()));
        report.setLostAt(trimToNull(report.getLostAt()));
        report.setLocation(trimToNull(report.getLocation()));
        report.setStorage(trimToNull(report.getStorage()));
        report.setMemo(trimToNull(report.getMemo()));
        if (report.getItemName() == null) {
            throw new IllegalArgumentException("itemName is required.");
        }
        if (report.getLocation() == null) {
            throw new IllegalArgumentException("location is required.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }


    private String resolveReportImage(
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

    private void validateStatusTransition(ReportStatus from, ReportStatus to) {
        if (from == to) return;
        if (from == ReportStatus.PICKED_UP && to != ReportStatus.PICKED_UP) {
            throw new IllegalStateException("최종 수령 완료 상태에서는 되돌릴 수 없습니다.");
        }
        if (from == ReportStatus.UNAVAILABLE && to == ReportStatus.PICKED_UP) {
            throw new IllegalStateException("습득 불가 상태에서는 바로 수령 완료로 변경할 수 없습니다.");
        }
    }

    private String safe(String value) {
        return value == null ? "미상 물품" : value;
    }
}
