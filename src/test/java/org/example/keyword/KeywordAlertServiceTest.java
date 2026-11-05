package org.example.keyword;

import org.example.auth.UserProfile;
import org.example.auth.UserProfileRepository;
import org.example.entity.ItemStatus;
import org.example.entity.LostItem;
import org.example.notice.NoticeService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KeywordAlertServiceTest {
    private final KeywordRepository keywords = mock(KeywordRepository.class);
    private final NoticeService notices = mock(NoticeService.class);
    private final UserProfileRepository users = mock(UserProfileRepository.class);
    private final KeywordAlertService service = new KeywordAlertService(keywords, notices, users);

    private Keyword keyword(String uid, String term) {
        Keyword k = new Keyword();
        k.setRequesterUid(uid);
        k.setKeyword(term);
        return k;
    }

    private void profile(String uid, boolean alertEnabled) {
        UserProfile p = new UserProfile();
        p.setFirebaseUid(uid);
        p.setKeywordAlertEnabled(alertEnabled);
        when(users.findByFirebaseUid(uid)).thenReturn(Optional.of(p));
    }

    @Test void usersWhoTurnedAlertsOffAreSkipped() {
        when(keywords.findAll()).thenReturn(List.of(keyword("u-on", "지갑"), keyword("u-off", "지갑")));
        profile("u-on", true);
        profile("u-off", false);
        LostItem item = new LostItem();
        item.setItemName("검정 지갑");
        item.setStatus(ItemStatus.ACQUIRED);

        service.notifyMatchingUsers(item);

        verify(notices).create(eq("u-on"), eq("관심 키워드 알림"), any());
        verify(notices, never()).create(eq("u-off"), any(), any());
    }
}
