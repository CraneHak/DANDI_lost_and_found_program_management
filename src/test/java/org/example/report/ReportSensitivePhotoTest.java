package org.example.report;

import org.example.adminlog.AdminActionLogService;
import org.example.notice.NoticeService;
import org.example.service.LostItemService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 검수 대기 신고를 승인(공개)할 때 신분증·카드는 모자이크 사진이 아니면 거부된다. */
class ReportSensitivePhotoTest {

    private final ReportRepository reports = mock(ReportRepository.class);
    private final NoticeService notices = mock(NoticeService.class);
    private final LostItemService lostItems = mock(LostItemService.class);
    private final AdminActionLogService logs = mock(AdminActionLogService.class);
    private final ReportService service = new ReportService(
            reports, notices, lostItems,
            mock(org.example.service.S3Service.class), mock(org.example.common.ImageUrlService.class), logs);
    private final ReportActor admin = new ReportActor("u-admin", "admin@dankook.ac.kr", true);

    private Report pending(long id, String category, String image) {
        Report r = new Report();
        r.setItemName("학생증");
        r.setLocation("도서관");
        r.setCategory(category);
        r.setImage(image);
        r.setStatus(ReportStatus.PENDING);
        org.springframework.test.util.ReflectionTestUtils.setField(r, "id", id);
        when(reports.findById(id)).thenReturn(Optional.of(r));
        when(reports.save(any(Report.class))).thenAnswer(c -> c.getArgument(0));
        return r;
    }

    @Test void approvingIdCardWithRawPhotoIsRejectedAndNothingChanges() {
        Report r = pending(31L, "신분증", "https://s3/raw.jpg");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateStatus(31L, ReportStatus.RESOLVED, admin));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(ReportStatus.PENDING, r.getStatus());
        verify(reports, never()).save(any());
        verify(lostItems, never()).upsertFromReport(any());
        verifyNoInteractions(notices);
        verifyNoInteractions(logs);
    }

    @Test void approvingCardWithRawPhotoIsRejected() {
        pending(32L, "카드/티켓", "https://s3/raw.jpg");
        assertThrows(ResponseStatusException.class, () -> service.updateStatus(32L, ReportStatus.RESOLVED, admin));
        verify(lostItems, never()).upsertFromReport(any());
    }

    @Test void approvingIdCardWithMosaicPhotoWorks() {
        Report r = pending(33L, "신분증", "/files/vision/mosaic/abc.jpeg");
        service.updateStatus(33L, ReportStatus.RESOLVED, admin);
        assertEquals(ReportStatus.RESOLVED, r.getStatus());
        verify(lostItems).upsertFromReport(r);
    }

    @Test void approvingOrdinaryItemWithRawPhotoWorks() {
        Report r = pending(34L, "화장품", "https://s3/raw.jpg");
        service.updateStatus(34L, ReportStatus.RESOLVED, admin);
        assertEquals(ReportStatus.RESOLVED, r.getStatus());
        verify(lostItems).upsertFromReport(r);
    }

    @Test void rejectingAsUnavailableIsNotBlockedForIdCard() {
        // 습득 불가 처리는 공개가 아니므로 막지 않는다
        Report r = pending(35L, "신분증", "https://s3/raw.jpg");
        assertDoesNotThrow(() -> service.updateStatus(35L, ReportStatus.UNAVAILABLE, admin));
        assertEquals(ReportStatus.UNAVAILABLE, r.getStatus());
    }
}
