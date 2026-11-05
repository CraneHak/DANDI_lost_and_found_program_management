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
    private final ReportService service =
            new ReportService(reports, mock(NoticeService.class), mock(org.example.service.LostItemService.class), null, null, logs);

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
}
