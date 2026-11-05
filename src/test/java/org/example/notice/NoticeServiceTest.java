package org.example.notice;

import org.example.auth.AdminDirectory;
import org.example.auth.FirebaseProperties;
import org.example.auth.UserProfile;
import org.example.auth.UserProfileRepository;
import org.example.auth.UserRole;
import org.example.report.Report;
import org.example.report.ReportActor;
import org.example.report.ReportRepository;
import org.example.report.ReportService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NoticeServiceTest {
    private final UserProfileRepository users = mock(UserProfileRepository.class);
    private final NoticeRepository notices = mock(NoticeRepository.class);
    private final FirebaseProperties props = new FirebaseProperties();
    private final NoticeService service = new NoticeService(notices, new AdminDirectory(users, props));

    private UserProfile profile(String uid, String email, UserRole role) {
        UserProfile p = new UserProfile();
        p.setFirebaseUid(uid);
        p.setEmail(email);
        p.setRole(role);
        when(users.findByFirebaseUid(uid)).thenReturn(Optional.of(p));
        return p;
    }

    {
        props.setAdminEmails(List.of("config-admin@dankook.ac.kr"));
        when(notices.save(any(Notice.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test void studentsStillGetNotices() {
        profile("u-student", "student@dankook.ac.kr", UserRole.ROLE_USER);
        assertNotNull(service.create("u-student", "습득 완료", "msg"));
        verify(notices).save(any(Notice.class));
    }

    @Test void dbAdminsDoNotGetRegularNotices() {
        profile("u-admin", "admin@dankook.ac.kr", UserRole.ROLE_ADMIN);
        assertNull(service.create("u-admin", "습득 완료", "msg"));
        verify(notices, never()).save(any());
    }

    @Test void configFileAdminsDoNotGetRegularNotices() {
        profile("u-config", "Config-Admin@dankook.ac.kr", UserRole.ROLE_USER);
        assertNull(service.create("u-config", "관심 키워드 알림", "msg"));
        verify(notices, never()).save(any());
    }

    @Test void newReportNoticeGoesToEveryAdminOnly() {
        UserProfile dbAdmin = profile("u-admin", "admin@dankook.ac.kr", UserRole.ROLE_ADMIN);
        UserProfile configAdmin = profile("u-config", "config-admin@dankook.ac.kr", UserRole.ROLE_USER);
        UserProfile student = profile("u-student", "student@dankook.ac.kr", UserRole.ROLE_USER);
        when(users.findAll()).thenReturn(List.of(dbAdmin, configAdmin, student));

        service.notifyAdmins("새 신고", "msg");

        verify(notices, times(2)).save(any(Notice.class));
        verify(notices, never()).save(argThat(n -> "u-student".equals(n.getRequesterUid())));
    }

    @Test void everyReportNotifiesAdmins() {
        NoticeService noticeService = mock(NoticeService.class);
        ReportRepository reports = mock(ReportRepository.class);
        when(reports.save(any(Report.class))).thenAnswer(call -> call.getArgument(0));
        ReportService reportService = new ReportService(reports, noticeService, null, null, null, mock(org.example.adminlog.AdminActionLogService.class));

        Report fromStudent = new Report();
        fromStudent.setItemName("지갑");
        fromStudent.setLocation("도서관");
        reportService.create(fromStudent, new ReportActor("u-student", "student@dankook.ac.kr", false));
        verify(noticeService).create(eq("u-student"), eq("내 신고가 접수되었습니다"), any());
        verify(noticeService).notifyAdmins(eq("새 신고"), any());

        // 관리자가 분실 신고 화면에서 한 신고도 관리자 전원에게 새 신고 알림
        clearInvocations(noticeService);
        Report adminReport = new Report();
        adminReport.setItemName("우산");
        adminReport.setLocation("학생회관");
        reportService.create(adminReport, new ReportActor("u-admin", "admin@dankook.ac.kr", true));
        verify(noticeService).notifyAdmins(eq("새 신고"), any());
    }
}
