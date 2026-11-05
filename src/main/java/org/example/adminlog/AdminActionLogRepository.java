package org.example.adminlog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {
    List<AdminActionLog> findTop200ByOrderByCreatedAtDescIdDesc();
}
