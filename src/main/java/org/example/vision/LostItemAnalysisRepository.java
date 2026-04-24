package org.example.vision;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LostItemAnalysisRepository extends JpaRepository<LostItemAnalysisRecord, Long> {
    List<LostItemAnalysisRecord> findTop20ByOrderByCreatedAtDesc();
}
