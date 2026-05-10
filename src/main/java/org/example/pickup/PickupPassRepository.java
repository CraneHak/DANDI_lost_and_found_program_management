package org.example.pickup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PickupPassRepository extends JpaRepository<PickupPass, Long> {
    @Query("select p from PickupPass p join fetch p.lostItem where p.token = :token")
    Optional<PickupPass> findByTokenForUpdate(@Param("token") String token);

    @Query("""
            select p from PickupPass p
            join fetch p.lostItem
            where p.lostItem.id = :lostItemId
              and p.requesterUid = :requesterUid
              and p.usedAt is null
              and p.expiresAt > :now
            order by p.issuedAt desc
            """)
    Optional<PickupPass> findLatestActivePass(
            @Param("lostItemId") Integer lostItemId,
            @Param("requesterUid") String requesterUid,
            @Param("now") OffsetDateTime now
    );
}
