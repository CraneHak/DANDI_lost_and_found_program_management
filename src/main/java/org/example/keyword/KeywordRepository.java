package org.example.keyword;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    List<Keyword> findAllByRequesterUidOrderByCreatedAtAsc(String requesterUid);
    Optional<Keyword> findByRequesterUidAndKeywordIgnoreCase(String requesterUid, String keyword);
    long countByRequesterUid(String requesterUid);
    Optional<Keyword> findByIdAndRequesterUid(Long id, String requesterUid);
}
