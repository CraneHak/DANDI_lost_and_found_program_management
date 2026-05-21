package org.example.pickup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionLogRepository extends JpaRepository<CollectionLog, Long> {

    @Modifying
    @Query("delete from CollectionLog c where c.lostItem.id = :lostItemId")
    void deleteByLostItemId(@Param("lostItemId") Integer lostItemId);
}
