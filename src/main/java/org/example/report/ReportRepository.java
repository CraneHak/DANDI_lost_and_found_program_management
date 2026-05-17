package org.example.report;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
    java.util.List<Report> findAllByOrderByCreatedAtDesc();
}
