package org.example.notice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByRequesterUidOrderByCreatedAtDesc(String requesterUid);
    java.util.Optional<Notice> findByIdAndRequesterUid(Long id, String requesterUid);
}
