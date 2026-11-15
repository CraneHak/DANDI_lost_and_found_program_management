package org.example.notice;

import org.example.auth.AdminDirectory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final AdminDirectory adminDirectory;

    public NoticeService(NoticeRepository noticeRepository, AdminDirectory adminDirectory) {
        this.noticeRepository = noticeRepository;
        this.adminDirectory = adminDirectory;
    }

    public List<Notice> findAll(String requesterUid) {
        return noticeRepository.findAllByRequesterUidOrderByCreatedAtDesc(requesterUid);
    }

    /**
     * 사용자 알림. 관리자는 새 신고 알림({@link #notifyAdmins})만 받으므로 관리자에게는 만들지 않는다.
     * @return 저장한 알림, 수신자가 관리자라 건너뛰면 null
     */
    @Transactional
    public Notice create(String requesterUid, String title, String message) {
        if (requesterUid == null || requesterUid.isBlank()) {
            throw new IllegalArgumentException("requesterUid is required.");
        }
        if (adminDirectory.isAdminUid(requesterUid)) {
            return null;
        }
        return save(requesterUid, title, message);
    }

    /**
     * 관심 키워드 알림. 일반 알림과 달리 관리자도 받는다 (관리자 계정으로도 키워드 알림을 확인할 수 있어야 함).
     */
    @Transactional
    public Notice createKeywordAlert(String requesterUid, String title, String message) {
        if (requesterUid == null || requesterUid.isBlank()) {
            throw new IllegalArgumentException("requesterUid is required.");
        }
        return save(requesterUid, title, message);
    }

    /** 관리자 전원에게 보내는 알림 (새 신고 접수용) */
    @Transactional
    public void notifyAdmins(String title, String message) {
        for (String adminUid : adminDirectory.adminUids()) {
            save(adminUid, title, message);
        }
    }

    private Notice save(String requesterUid, String title, String message) {
        Notice notice = new Notice();
        notice.setRequesterUid(requesterUid);
        notice.setTitle(title);
        notice.setMessage(message);
        notice.setRead(false);
        return noticeRepository.save(notice);
    }

    @Transactional
    public Notice markRead(String requesterUid, Long id) {
        Notice notice = noticeRepository.findByIdAndRequesterUid(id, requesterUid)
                .orElseThrow(() -> new NoticeNotFoundException(id));
        notice.setRead(true);
        return noticeRepository.save(notice);
    }

    @Transactional
    public void delete(String requesterUid, Long id) {
        Notice notice = noticeRepository.findByIdAndRequesterUid(id, requesterUid)
                .orElseThrow(() -> new NoticeNotFoundException(id));
        noticeRepository.delete(notice);
    }

    @Transactional
    public void deleteAll(String requesterUid) {
        noticeRepository.deleteAllByRequesterUid(requesterUid);
    }
}
