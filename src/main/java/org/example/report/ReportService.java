package org.example.report;

import org.example.notice.Notice;
import org.example.notice.NoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final NoticeRepository noticeRepository;

    public ReportService(ReportRepository reportRepository, NoticeRepository noticeRepository) {
        this.reportRepository = reportRepository;
        this.noticeRepository = noticeRepository;
    }

    public List<Report> findAll() {
        return reportRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Report create(Report report) {
        normalizeReportFields(report);
        Report saved = reportRepository.save(report);
        createNotice(
                "신고 접수됨",
                "분실물 신고가 정상 접수되었습니다: " + safe(saved.getItemName())
        );
        return saved;
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
        createNotice(
                switch (newStatus) {
                    case RESOLVED -> "습득 완료";
                    case PICKED_UP -> "최종 수령 완료";
                    case UNAVAILABLE -> "습득 불가";
                    default -> "신고 상태 변경";
                },
                "신고 상태가 '" + newStatus.getValue() + "'로 변경되었습니다: " + safe(saved.getItemName())
        );
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ReportNotFoundException(id));
        if (report.getStatus() == ReportStatus.PICKED_UP) {
            throw new IllegalStateException("최종 수령 완료된 신고는 삭제할 수 없습니다.");
        }
        reportRepository.deleteById(id);
    }

    private void normalizeReportFields(Report report) {
        report.setItemName(trimToNull(report.getItemName()));
        report.setCategory(trimToNull(report.getCategory()));
        report.setLostAt(trimToNull(report.getLostAt()));
        report.setLocation(trimToNull(report.getLocation()));
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

    private void validateStatusTransition(ReportStatus from, ReportStatus to) {
        if (from == to) return;
        if (from == ReportStatus.PICKED_UP && to != ReportStatus.PICKED_UP) {
            throw new IllegalStateException("최종 수령 완료 상태에서는 되돌릴 수 없습니다.");
        }
        if (from == ReportStatus.UNAVAILABLE && to == ReportStatus.PICKED_UP) {
            throw new IllegalStateException("습득 불가 상태에서는 바로 수령 완료로 변경할 수 없습니다.");
        }
    }

    private void createNotice(String title, String message) {
        Notice notice = new Notice();
        notice.setTitle(title);
        notice.setMessage(message);
        noticeRepository.save(notice);
    }

    private String safe(String value) {
        return value == null ? "미상 물품" : value;
    }
}
