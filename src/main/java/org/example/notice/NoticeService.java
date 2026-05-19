package org.example.notice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    public List<Notice> findAll(String requesterUid) {
        return noticeRepository.findAllByRequesterUidOrderByCreatedAtDesc(requesterUid);
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
}
