package org.example.pickup;

import org.example.auth.FirebaseAuthenticationToken;
import org.example.entity.ItemStatus;
import org.example.entity.LostItem;
import org.example.notice.NoticeService;
import org.example.report.Report;
import org.example.report.ReportRepository;
import org.example.report.ReportStatus;
import org.example.repository.LostItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PickupPassServiceTest {
    private final LostItemRepository lostItemRepository = mock(LostItemRepository.class);
    private final PickupPassRepository pickupPassRepository = mock(PickupPassRepository.class);
    private final ReportRepository reportRepository = mock(ReportRepository.class);
    private final PickupPassService service = new PickupPassService(
            lostItemRepository,
            pickupPassRepository,
            mock(CollectionLogRepository.class),
            reportRepository,
            mock(NoticeService.class)
    );
    private final FirebaseAuthenticationToken user = new FirebaseAuthenticationToken("u1", "u1@dankook.ac.kr", false);
    private final FirebaseAuthenticationToken admin = new FirebaseAuthenticationToken("a1", "a1@dankook.ac.kr", true);

    private LostItem item;
    private Report report;

    @BeforeEach
    void setUp() {
        item = new LostItem();
        item.setId(10);
        item.setReportId(20L);
        item.setItemName("지갑");
        item.setStatus(ItemStatus.ACQUIRED);
        report = new Report();
        report.setId(20L);
        report.setStatus(ReportStatus.RESOLVED);

        when(lostItemRepository.findById(10)).thenReturn(Optional.of(item));
        when(lostItemRepository.findByIdForUpdate(10)).thenReturn(Optional.of(item));
        when(reportRepository.findById(20L)).thenReturn(Optional.of(report));
        when(pickupPassRepository.save(any(PickupPass.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void issueKeepsItemStatusAndReturnsRealReportId() {
        var response = service.issuePass(user, new PickupPassService.IssuePickupPassRequest("10"));

        assertEquals(ItemStatus.ACQUIRED, item.getStatus());
        assertEquals("10", response.lostItemId());
        assertEquals("20", response.reportId());
    }

    @Test
    void issueRejectsReceivedItem() {
        item.setStatus(ItemStatus.RECEIVED);

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.issuePass(user, new PickupPassService.IssuePickupPassRequest("10")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(ItemStatus.RECEIVED, item.getStatus());
    }

    @Test
    void verifyMarksItemReceivedAndLinkedReportPickedUp() {
        PickupPass pass = activePass("DKU-A");

        var response = service.verifyPass(admin, new PickupPassService.VerifyPickupPassRequest("DKU-A"));

        assertEquals(ItemStatus.RECEIVED, item.getStatus());
        assertEquals(ReportStatus.PICKED_UP, report.getStatus());
        assertNotNull(report.getPickedUpAt());
        assertNotNull(pass.getUsedAt());
        assertEquals("20", response.reportId());
    }

    @Test
    void verifyRejectsSecondPassForReceivedItem() {
        activePass("DKU-B");
        item.setStatus(ItemStatus.RECEIVED);

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.verifyPass(admin, new PickupPassService.VerifyPickupPassRequest("DKU-B")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    private PickupPass activePass(String token) {
        PickupPass pass = new PickupPass();
        pass.setToken(token);
        pass.setLostItem(item);
        pass.setRequesterUid("u1");
        pass.setIssuedAt(OffsetDateTime.now());
        pass.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        when(pickupPassRepository.findByTokenForUpdate(token)).thenReturn(Optional.of(pass));
        return pass;
    }
}
