package org.example.favorite;

import org.example.repository.LostItemRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FavoriteItemServiceTest {
    private final FavoriteItemRepository favoriteItemRepository = mock(FavoriteItemRepository.class);
    private final LostItemRepository lostItemRepository = mock(LostItemRepository.class);
    private final FavoriteItemService service = new FavoriteItemService(favoriteItemRepository, lostItemRepository);

    @Test
    void 없는_물품은_저장하지_않는다() {
        when(lostItemRepository.existsById(10)).thenReturn(false);
        assertThrows(FavoriteItemNotFoundException.class, () -> service.add("u1", 10));
        verify(favoriteItemRepository, never()).save(any());
    }

    @Test
    void 이미_저장된_물품은_다시_저장하지_않는다() {
        when(lostItemRepository.existsById(10)).thenReturn(true);
        when(favoriteItemRepository.existsByRequesterUidAndLostItemId("u1", 10)).thenReturn(true);
        service.add("u1", 10);
        verify(favoriteItemRepository, never()).save(any());
    }

    @Test
    void 새_물품은_요청자_계정으로_저장한다() {
        when(lostItemRepository.existsById(10)).thenReturn(true);
        service.add("u1", 10);
        verify(favoriteItemRepository).save(argThat(f -> "u1".equals(f.getRequesterUid()) && f.getLostItemId() == 10));
    }

    @Test
    void 최대_개수를_넘으면_거부한다() {
        when(lostItemRepository.existsById(10)).thenReturn(true);
        when(favoriteItemRepository.countByRequesterUid("u1")).thenReturn(100L);
        assertThrows(IllegalArgumentException.class, () -> service.add("u1", 10));
    }
}
