package org.example.pickup;

import org.example.auth.FirebaseAuthenticationToken;
import org.example.entity.ItemStatus;
import org.example.entity.LostItem;
import org.example.notice.NoticeService;
import org.example.report.Report;
import org.example.report.ReportRepository;
import org.example.report.ReportStatus;
import org.example.repository.LostItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PickupPassService {
    private static final long DEFAULT_TTL_MINUTES = 10;
    private final LostItemRepository lostItemRepository;
    private final PickupPassRepository pickupPassRepository;
    private final CollectionLogRepository collectionLogRepository;
    private final ReportRepository reportRepository;
    private final NoticeService noticeService;

    public PickupPassService(
            LostItemRepository lostItemRepository,
            PickupPassRepository pickupPassRepository,
            CollectionLogRepository collectionLogRepository,
            ReportRepository reportRepository,
            NoticeService noticeService
    ) {
        this.lostItemRepository = lostItemRepository;
        this.pickupPassRepository = pickupPassRepository;
        this.collectionLogRepository = collectionLogRepository;
        this.reportRepository = reportRepository;
        this.noticeService = noticeService;
    }

    /**
     * 공개(ACQUIRED) 물품에만 발급한다. 물품 상태는 바꾸지 않으므로 QR이 만료돼도 되돌릴 상태가 없다.
     * PENDING_RECEIPT는 이전 버전이 발급 시 남긴 값이라 발급 가능으로 취급한다.
     */
    @Transactional
    public IssuePickupPassResponse issuePass(FirebaseAuthenticationToken requester, IssuePickupPassRequest request) {
        Integer lostItemId = parseLostItemId(request);
        LostItem lostItem = lostItemRepository.findById(lostItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lost item not found."));
        requireIssuable(lostItem);

        OffsetDateTime now = OffsetDateTime.now();
        PickupPass activePass = pickupPassRepository
                .findFirstByLostItem_IdAndRequesterUidAndUsedAtIsNullAndExpiresAtAfterOrderByIssuedAtDesc(
                        lostItemId, requester.getUid(), now)
                .orElse(null);
        if (activePass != null) {
            return toIssueResponse(activePass, "이미 발급된 유효한 수령 QR이 있습니다.");
        }

        PickupPass pass = new PickupPass();
        pass.setToken(newToken());
        pass.setLostItem(lostItem);
        pass.setRequesterUid(requester.getUid());
        pass.setRequesterEmail(requester.getEmail());
        pass.setIssuedAt(now);
        pass.setExpiresAt(now.plusMinutes(DEFAULT_TTL_MINUTES));

        PickupPass saved = pickupPassRepository.save(pass);

        CollectionLog issuedLog = new CollectionLog();
        issuedLog.setLostItem(lostItem);
        issuedLog.setPickupPassId(saved.getId());
        issuedLog.setRequesterUid(saved.getRequesterUid());
        issuedLog.setRequesterEmail(saved.getRequesterEmail());
        issuedLog.setOtpToken(saved.getToken());
        issuedLog.setOtpExpiresAt(saved.getExpiresAt());
        issuedLog.setAction(CollectionAction.QR_ISSUED);
        collectionLogRepository.save(issuedLog);

        return toIssueResponse(saved, "수령 QR이 발급되었습니다.");
    }

    /**
     * 토큰과 물품 행을 잠근 뒤 검증한다. 같은 물품의 다른 QR이 먼저 인증됐으면 거부한다.
     * 물품 RECEIVED, 연결 신고 PICKED_UP, 수령 이력을 한 트랜잭션에서 확정한다.
     */
    @Transactional
    public VerifyPickupPassResponse verifyPass(FirebaseAuthenticationToken verifier, VerifyPickupPassRequest request) {
        requireAdmin(verifier);
        String token = normalizeToken(request.token());
        PickupPass pass = pickupPassRepository.findByTokenForUpdate(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 QR 토큰입니다."));

        OffsetDateTime now = OffsetDateTime.now();
        if (pass.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용된 QR 토큰입니다.");
        }
        if (pass.getExpiresAt().isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "만료된 QR 토큰입니다.");
        }

        LostItem lostItem = lostItemRepository.findByIdForUpdate(pass.getLostItem().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lost item not found."));
        if (lostItem.getStatus() == ItemStatus.RECEIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 수령 완료된 물품입니다.");
        }

        pass.setUsedAt(now);
        pass.setVerifiedByUid(verifier.getUid());
        pass.setVerifiedByEmail(verifier.getEmail());
        lostItem.setStatus(ItemStatus.RECEIVED);
        PickupPass saved = pickupPassRepository.save(pass);

        Report report = markLinkedReportPickedUp(lostItem);

        CollectionLog verifyLog = new CollectionLog();
        verifyLog.setLostItem(lostItem);
        verifyLog.setPickupPassId(saved.getId());
        verifyLog.setRequesterUid(saved.getRequesterUid());
        verifyLog.setRequesterEmail(saved.getRequesterEmail());
        verifyLog.setManagerUid(saved.getVerifiedByUid());
        verifyLog.setManagerEmail(saved.getVerifiedByEmail());
        verifyLog.setOtpToken(saved.getToken());
        verifyLog.setOtpExpiresAt(saved.getExpiresAt());
        verifyLog.setCollectedAt(saved.getUsedAt());
        verifyLog.setAction(CollectionAction.QR_VERIFIED);
        collectionLogRepository.save(verifyLog);

        notifyPickedUp(saved, report, lostItem);

        return new VerifyPickupPassResponse(
                true,
                "수령 인증이 완료되었습니다.",
                String.valueOf(lostItem.getId()),
                report != null ? String.valueOf(report.getId()) : null,
                saved.getRequesterEmail(),
                saved.getUsedAt().toString()
        );
    }

    @Transactional(readOnly = true)
    public List<MyPickupPassResponse> findMyPasses(FirebaseAuthenticationToken requester) {
        return pickupPassRepository.findAllByRequesterUidWithItem(requester.getUid()).stream()
                .map(pass -> {
                    LostItem item = pass.getLostItem();
                    return new MyPickupPassResponse(
                            String.valueOf(pass.getId()),
                            String.valueOf(item.getId()),
                            item.getReportId() != null ? String.valueOf(item.getReportId()) : null,
                            item.getItemName(),
                            item.getStoredLocation(),
                            pass.getRequesterEmail(),
                            pass.getToken(),
                            pass.getIssuedAt().toString(),
                            pass.getExpiresAt().toString(),
                            pass.getUsedAt() == null ? null : pass.getUsedAt().toString()
                    );
                })
                .toList();
    }

    private void requireIssuable(LostItem lostItem) {
        ItemStatus status = lostItem.getStatus();
        if (status == ItemStatus.RECEIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 수령 완료된 물품입니다.");
        }
        if (status != ItemStatus.ACQUIRED && status != ItemStatus.PENDING_RECEIPT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "아직 수령할 수 없는 물품입니다.");
        }
    }

    private Report markLinkedReportPickedUp(LostItem lostItem) {
        if (lostItem.getReportId() == null) {
            return null;
        }
        Report report = reportRepository.findById(lostItem.getReportId()).orElse(null);
        if (report != null && report.getStatus() != ReportStatus.PICKED_UP) {
            report.setStatus(ReportStatus.PICKED_UP);
            report.setPickedUpAt(LocalDateTime.now());
            reportRepository.save(report);
        }
        return report;
    }

    private void notifyPickedUp(PickupPass pass, Report report, LostItem lostItem) {
        String message = "물품 수령이 완료되었습니다: " + (lostItem.getItemName() == null ? "미상 물품" : lostItem.getItemName());
        if (pass.getRequesterUid() != null && !pass.getRequesterUid().isBlank()) {
            noticeService.create(pass.getRequesterUid(), "최종 수령 완료", message);
        }
        String reporterUid = report != null ? report.getReporterUid() : null;
        if (reporterUid != null && !reporterUid.isBlank() && !reporterUid.equals(pass.getRequesterUid())) {
            noticeService.create(reporterUid, "최종 수령 완료", message);
        }
    }

    private Integer parseLostItemId(IssuePickupPassRequest request) {
        String rawId = request.lostItemId();
        if (rawId == null || rawId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lostItemId is required.");
        }
        try {
            return Integer.valueOf(rawId.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lostItemId must be numeric.");
        }
    }

    private String newToken() {
        return "DKU-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token is required.");
        }
        return rawToken.trim();
    }

    private void requireAdmin(FirebaseAuthenticationToken auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required.");
        }
    }

    private IssuePickupPassResponse toIssueResponse(PickupPass pass, String message) {
        LostItem item = pass.getLostItem();
        return new IssuePickupPassResponse(
                String.valueOf(pass.getId()),
                String.valueOf(item.getId()),
                item.getReportId() != null ? String.valueOf(item.getReportId()) : null,
                pass.getToken(),
                pass.getIssuedAt().toString(),
                pass.getExpiresAt().toString(),
                pass.getUsedAt() == null ? null : pass.getUsedAt().toString(),
                message
        );
    }

    public record IssuePickupPassRequest(String lostItemId) {
    }

    /** reportId는 물품에 연결된 실제 신고 ID (없으면 null). */
    public record IssuePickupPassResponse(
            String id,
            String lostItemId,
            String reportId,
            String token,
            String issuedAt,
            String expiresAt,
            String usedAt,
            String message
    ) {
    }

    public record VerifyPickupPassRequest(String token) {
    }

    public record VerifyPickupPassResponse(
            boolean ok,
            String message,
            String lostItemId,
            String reportId,
            String requesterEmail,
            String usedAt
    ) {
    }

    public record MyPickupPassResponse(
            String id,
            String lostItemId,
            String reportId,
            String itemName,
            String itemLocation,
            String claimantEmail,
            String token,
            String issuedAt,
            String expiresAt,
            String usedAt
    ) {
    }
}
