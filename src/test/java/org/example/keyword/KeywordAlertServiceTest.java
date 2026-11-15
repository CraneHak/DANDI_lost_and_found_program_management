package org.example.keyword;

import org.example.auth.UserProfile;
import org.example.auth.UserProfileRepository;
import org.example.entity.ItemStatus;
import org.example.entity.LostItem;
import org.example.notice.NoticeService;
import org.example.repository.LostItemRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KeywordAlertServiceTest {
    private final KeywordRepository keywords = mock(KeywordRepository.class);
    private final NoticeService notices = mock(NoticeService.class);
    private final UserProfileRepository users = mock(UserProfileRepository.class);
    private final LostItemRepository items = mock(LostItemRepository.class);
    private final KeywordAlertService service = new KeywordAlertService(keywords, notices, users, items);

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

    private LostItem item(String name, String place) {
        LostItem item = new LostItem();
        item.setItemName(name);
        item.setFoundLocation(place);
        item.setStatus(ItemStatus.ACQUIRED);
        return item;
    }

    @Test void usersWhoTurnedAlertsOffAreSkipped() {
        when(keywords.findAll()).thenReturn(List.of(keyword("u-on", "지갑"), keyword("u-off", "지갑")));
        profile("u-on", true);
        profile("u-off", false);

        service.notifyMatchingUsers(item("검정 지갑", "도서관"));

        verify(notices).createKeywordAlert(eq("u-on"), eq("관심 키워드 알림"), any());
        verify(notices, never()).createKeywordAlert(eq("u-off"), any(), any());
    }

    // 관리자 계정도 같은 경로로 알림을 받는다 (관리자 제외 규칙은 일반 create()에만 있다)
    @Test void newlyPublishedItemAlertsAdminKeywordOwnersToo() {
        when(keywords.findAll()).thenReturn(List.of(keyword("u-admin", "마우스")));

        service.notifyMatchingUsers(item("무선 마우스", "ICT관"));

        verify(notices).createKeywordAlert(eq("u-admin"), eq("관심 키워드 알림"), any());
        verify(notices, never()).create(any(), any(), any());
    }

    @Test void newKeywordSendsOneNoticeListingExistingMatches() {
        when(items.findByStatusOrderByIdDesc(ItemStatus.ACQUIRED))
                .thenReturn(List.of(item("흰색 마우스", "ICT관"), item("검정 지갑", "도서관"), item("무선 마우스", "인문관")));

        service.notifyExistingItems("u-1", "마우스");

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notices, times(1)).createKeywordAlert(eq("u-1"), eq("관심 키워드 알림"), message.capture());
        String text = message.getValue();
        assertTrue(text.contains("2건"), text);
        assertTrue(text.contains("흰색 마우스") && text.contains("무선 마우스"), text);
        assertTrue(!text.contains("검정 지갑"), text);
    }

    @Test void newKeywordWithoutMatchesSendsNothing() {
        when(items.findByStatusOrderByIdDesc(ItemStatus.ACQUIRED)).thenReturn(List.of(item("검정 지갑", "도서관")));

        service.notifyExistingItems("u-1", "마우스");

        verify(notices, never()).createKeywordAlert(any(), any(), any());
    }

    @Test void newKeywordRespectsAlertPreference() {
        profile("u-off", false);

        service.notifyExistingItems("u-off", "마우스");

        verify(notices, never()).createKeywordAlert(any(), any(), any());
        verifyNoInteractions(items);
    }

    @Test void newKeywordNoticeListsFiveItemsAndSummarizesTheRest() {
        List<LostItem> many = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            many.add(item("마우스" + i, "ICT관"));
        }
        when(items.findByStatusOrderByIdDesc(ItemStatus.ACQUIRED)).thenReturn(many);

        service.notifyExistingItems("u-1", "마우스");

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notices, times(1)).createKeywordAlert(eq("u-1"), any(), message.capture());
        String text = message.getValue();
        assertTrue(text.contains("7건"), text);
        assertTrue(text.contains("마우스5") && !text.contains("마우스6"), text);
        assertTrue(text.contains("외 2건"), text);
    }
}
