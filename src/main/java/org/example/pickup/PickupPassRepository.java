package org.example.pickup;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PickupPassRepository extends JpaRepository<PickupPass, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PickupPass p where p.token = :token")
    Optional<PickupPass> findByTokenForUpdate(@Param("token") String token);

    Optional<PickupPass> findFirstByLostItem_IdAndRequesterUidAndUsedAtIsNullAndExpiresAtAfterOrderByIssuedAtDesc(
            Integer lostItemId,
            String requesterUid,
            OffsetDateTime now
    );

    @Query("select p from PickupPass p join fetch p.lostItem where p.requesterUid = :requesterUid order by p.issuedAt desc")
    List<PickupPass> findAllByRequesterUidWithItem(@Param("requesterUid") String requesterUid);

    void deleteByLostItem_Id(Integer lostItemId);
}
