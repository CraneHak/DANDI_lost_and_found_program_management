package org.example.repository;

import jakarta.persistence.LockModeType;
import org.example.entity.LostItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LostItemRepository extends JpaRepository<LostItem, Integer> {
    Optional<LostItem> findByReportId(Long reportId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from LostItem i where i.id = :id")
    Optional<LostItem> findByIdForUpdate(@Param("id") Integer id);
}
