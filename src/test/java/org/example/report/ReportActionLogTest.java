package org.example.report;

import org.example.adminlog.AdminActionLogService;
import org.example.notice.NoticeService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReportActionLogTest {
    private final ReportRepository reports = mock(ReportRepository.class);
    private final AdminActionLogService logs = mock(AdminActionLogService.class);
    private final org.example.service.S3Service s3 = mock(org.example.service.S3Service.class);
    private final org.example.common.ImageUrlService imageUrls = mock(org.example.common.ImageUrlService.class);
    private final ReportService service =
            new ReportService(reports, mock(NoticeService.class), mock(org.example.service.LostItemService.class), s3, imageUrls, logs);

    private Report pending(long id, String name) {
        Report r = new Report();
        r.setItemName(name);
        r.setLocation("도서관");
        r.setStatus(ReportStatus.PENDING);
        org.springframework.test.util.ReflectionTestUtils.setField(r, "id", id);
        when(reports.findById(id)).thenReturn(Optional.of(r));
        when(reports.save(any(Report.class))).thenAnswer(call -> call.getArgument(0));
        return r;
    }

    @Test void adminReviewIsRecordedInActionLog() {
        pending(7L, "우산");
        service.updateStatus(7L, ReportStatus.UNAVAILABLE, new ReportActor("u-admin", "admin@dankook.ac.kr", true));
        verify(logs).record(eq("u-admin"), eq("admin@dankook.ac.kr"), eq(AdminActionLogService.REVIEW),
                eq("report-7"), contains("습득 불가"));
    }

    @Test void studentDeletingOwnReportIsNotAnAdminAction() {
        pending(8L, "지갑").setReporterUid("u-student");
        service.delete(8L, new ReportActor("u-student", "student@dankook.ac.kr", false));
        verify(logs, never()).record(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test void adminDeleteIsRecorded() {
        pending(9L, "모자");
        service.delete(9L, new ReportActor("u-admin", "admin@dankook.ac.kr", true));
        verify(logs).record(eq("u-admin"), any(), eq(AdminActionLogService.DELETE_REPORT), eq("report-9"), contains("모자"));
    }

    @Test void studentCannotDeleteSomeoneElsesReport() {
        pending(10L, "가방").setReporterUid("u-other");
        org.junit.jupiter.api.Assertions.assertThrows(SecurityException.class,
                () -> service.delete(10L, new ReportActor("u-student", "student@dankook.ac.kr", false)));
        verify(reports, never()).deleteById(10L);
    }

    @Test void studentCannotDeleteOwnReportAfterReview() {
        Report r = pending(11L, "우산");
        r.setReporterUid("u-student");
        r.setStatus(ReportStatus.RESOLVED);
        org.junit.jupiter.api.Assertions.assertThrows(SecurityException.class,
                () -> service.delete(11L, new ReportActor("u-student", "student@dankook.ac.kr", false)));
        verify(reports, never()).deleteById(11L);
    }

    private static final String S3_ORIGINAL = "https://dandi-lost-items.s3.ap-northeast-2.amazonaws.com/lost-items/a.jpg";

    @Test void deletingReportAlsoDeletesItsS3Photo() {
        Report r = pending(12L, "지갑");
        r.setImage(S3_ORIGINAL);
        service.delete(12L, new ReportActor("u-admin", "admin@dankook.ac.kr", true));
        verify(s3).deleteByUrlIfPresent(S3_ORIGINAL);
    }

    @Test void originalIsDeletedOnlyWhenReplacedByMosaic() {
        Report r = pending(13L, "학생증");
        r.setImage(S3_ORIGINAL);
        when(imageUrls.normalizeForStorage("/files/vision/mosaic/m.png")).thenReturn("/files/vision/mosaic/m.png");
        UpdateReportDetailsRequest toMosaic = new com.fasterxml.jackson.databind.ObjectMapper().convertValue(
                java.util.Map.of("image", "/files/vision/mosaic/m.png"), UpdateReportDetailsRequest.class);
        service.updateDetails(13L, toMosaic, new ReportActor("u-admin", "admin@dankook.ac.kr", true));
        verify(s3).deleteByUrlIfPresent(S3_ORIGINAL);
    }

    @Test void sameOriginalIsNotDeletedWhenNotMosaic() {
        Report r = pending(14L, "지갑");
        r.setImage(S3_ORIGINAL);
        when(imageUrls.normalizeForStorage(S3_ORIGINAL)).thenReturn(S3_ORIGINAL);
        UpdateReportDetailsRequest same = new com.fasterxml.jackson.databind.ObjectMapper().convertValue(
                java.util.Map.of("image", S3_ORIGINAL), UpdateReportDetailsRequest.class);
        service.updateDetails(14L, same, new ReportActor("u-admin", "admin@dankook.ac.kr", true));
        verify(s3, never()).deleteByUrlIfPresent(S3_ORIGINAL);
    }
}
