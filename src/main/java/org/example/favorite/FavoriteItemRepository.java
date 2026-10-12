package org.example.favorite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteItemRepository extends JpaRepository<FavoriteItem, Long> {
    List<FavoriteItem> findAllByRequesterUidOrderByCreatedAtAsc(String requesterUid);
    boolean existsByRequesterUidAndLostItemId(String requesterUid, Integer lostItemId);
    long countByRequesterUid(String requesterUid);
    void deleteByRequesterUidAndLostItemId(String requesterUid, Integer lostItemId);
}
