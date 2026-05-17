package org.example.keyword;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    List<Keyword> findAllByOrderByCreatedAtAsc();
    Optional<Keyword> findByKeywordIgnoreCase(String keyword);
}
