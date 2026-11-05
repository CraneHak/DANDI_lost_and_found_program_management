package org.example.report;

import org.example.adminlog.AdminActionLogService;
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
    private final AdminActionLogService adminActionLogService;

    public ReportService(
            ReportRepository reportRepository,
            NoticeService noticeService,
            LostItemService lostItemService,
            S3Service s3Service,
            ImageUrlService imageUrlService,
            AdminActionLogService adminActionLogService
    ) {
        this.reportRepository = reportRepository;
        this.noticeService = noticeService;
        this.lostItemService = lostItemService;
        this.s3Service = s3Service;
        this.imageUrlService = imageUrlService;
        this.adminActionLogService = adminActionLogService;
    }

    public List<Report> findAll() {
        return reportRepository.findAllByOrderByCreatedAtDesc();
    }

    /** 관리자는 전체, 일반 사용자는 본인 신고만 (다른 사람의 이메일·원본 사진 보호) */
    public List<Report> findAllVisibleTo(ReportActor actor) {
        List<Report> all = findAll();
        if (actor.admin()) {
            return all;
        }
        return all.stream().filter(report -> isOwner(report, actor)).toList();
    }

    private boolean isOwner(Report report, ReportActor actor) {
        if (report.getReporterUid() != null && !report.getReporterUid().isBlank()) {
            return report.getReporterUid().equals(actor.uid());
        }
        // uid가 없는 예전 신고는 이메일로 판단
        return report.getReporterEmail() != null && actor.email() != null
                && report.getReporterEmail().equalsIgnoreCase(actor.email());
    }

    /** 누가 신고했든 관리자 전원에게 새 신고 알림 (관리자 물품 직접 등록은 신고를 만들지 않는다) */
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
            // 신고자가 관리자면 NoticeService가 이 사용자용 알림을 건너뛴다
            noticeService.create(
                    actor.uid(),
                    "내 신고가 접수되었습니다",
                    "신고하신 물품: " + safe(saved.getItemName())
                            + "\n관리자가 확인한 뒤 결과를 알림으로 알려드립니다."
            );
            noticeService.notifyAdmins(
                    "새 신고",
                    "새 분실물 신고가 들어왔습니다: " + safe(saved.getItemName())
                            + " (" + safe(saved.getLocation()) + ")\n관리자 화면의 검수 대기에서 확인해 주세요."
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
        String replacedOriginal = null;
        if (image != null) {
            String stored = imageUrlService.normalizeForStorage(image);
            // 모자이크로 바뀔 때만 이전 원본을 지운다 (같은 사진을 다른 표기로 보낸 경우 지우지 않도록)
            if (isMosaic(stored) && report.getImage() != null && !isMosaic(report.getImage())) {
                replacedOriginal = report.getImage();
            }
            report.setImage(stored);
        }

        normalizeReportFields(report);
        Report saved = reportRepository.save(report);
        s3Service.deleteByUrlIfPresent(replacedOriginal);
        logAdmin(actor, AdminActionLogService.EDIT_REPORT, saved,
                "[" + safe(saved.getItemName()) + "] 검수 대기 신고 내용 수정");
        return saved;
    }

    @Transactional
    public Report updateStatus(Long id, ReportStatus newStatus, ReportActor actor) {
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
        logAdmin(actor, AdminActionLogService.REVIEW, saved,
                "[" + safe(saved.getItemName()) + "] 신고를 " + statusLabel(newStatus) + "(으)로 처리");

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
    public void delete(Long id, ReportActor actor) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ReportNotFoundException(id));
        if (actor == null || !actor.admin()) {
            if (actor == null || !isOwner(report, actor)) {
                throw new SecurityException("본인 신고만 삭제할 수 있습니다.");
            }
            // 공개된 물품까지 지워지지 않도록 일반 사용자는 검수 전 신고만 (등록 화면도 검수 대기 신고만 보여줌)
            if (report.getStatus() != ReportStatus.PENDING) {
                throw new SecurityException("검수가 끝난 신고는 관리자에게 삭제를 요청해 주세요.");
            }
        }
        if (report.getStatus() == ReportStatus.PICKED_UP) {
            throw new IllegalStateException("최종 수령 완료된 신고는 삭제할 수 없습니다.");
        }
        lostItemService.deleteByReportId(id);
        reportRepository.deleteById(id);
        // 신고 사진이 S3에 주인 없이 남지 않게 함께 지운다 (연결 물품 사진은 deleteByReportId가 지움)
        s3Service.deleteByUrlIfPresent(report.getImage());
        logAdmin(actor, AdminActionLogService.DELETE_REPORT, report,
                "[" + safe(report.getItemName()) + "] 신고 삭제");
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

    /** 관리자가 한 작업만 작업 이력에 남긴다 */
    private void logAdmin(ReportActor actor, String action, Report report, String message) {
        if (actor == null || !actor.admin()) {
            return;
        }
        adminActionLogService.record(actor.uid(), actor.email(), action,
                report.getId() != null ? "report-" + report.getId() : null, message);
    }

    private static boolean isMosaic(String imageUrl) {
        return imageUrl != null && imageUrl.contains("/vision/mosaic/");
    }

    private String statusLabel(ReportStatus status) {
        return switch (status) {
            case RESOLVED -> "습득 완료";
            case UNAVAILABLE -> "습득 불가";
            case PICKED_UP -> "최종 수령 완료";
            case PENDING -> "검수 대기";
        };
    }

    private String safe(String value) {
        return value == null ? "미상 물품" : value;
    }
}
