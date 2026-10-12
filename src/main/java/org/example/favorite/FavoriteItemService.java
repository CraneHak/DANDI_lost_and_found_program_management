package org.example.favorite;

import org.example.repository.LostItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FavoriteItemService {
    private static final int MAX_FAVORITE_COUNT = 100;

    private final FavoriteItemRepository favoriteItemRepository;
    private final LostItemRepository lostItemRepository;

    public FavoriteItemService(FavoriteItemRepository favoriteItemRepository, LostItemRepository lostItemRepository) {
        this.favoriteItemRepository = favoriteItemRepository;
        this.lostItemRepository = lostItemRepository;
    }

    public List<FavoriteItem> findAll(String requesterUid) {
        return favoriteItemRepository.findAllByRequesterUidOrderByCreatedAtAsc(requesterUid);
    }

    /** 이미 저장된 물품이면 그대로 둔다 (하트를 여러 번 눌러도 1건) */
    @Transactional
    public void add(String requesterUid, Integer lostItemId) {
        if (!lostItemRepository.existsById(lostItemId)) {
            throw new FavoriteItemNotFoundException(lostItemId);
        }
        if (favoriteItemRepository.existsByRequesterUidAndLostItemId(requesterUid, lostItemId)) {
            return;
        }
        if (favoriteItemRepository.countByRequesterUid(requesterUid) >= MAX_FAVORITE_COUNT) {
            throw new IllegalArgumentException("you can save up to 100 favorite items.");
        }
        FavoriteItem entity = new FavoriteItem();
        entity.setRequesterUid(requesterUid);
        entity.setLostItemId(lostItemId);
        favoriteItemRepository.save(entity);
    }

    /** 없는 항목을 지워도 성공으로 본다 */
    @Transactional
    public void remove(String requesterUid, Integer lostItemId) {
        favoriteItemRepository.deleteByRequesterUidAndLostItemId(requesterUid, lostItemId);
    }
}
