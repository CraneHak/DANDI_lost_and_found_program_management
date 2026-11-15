package org.example.keyword;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KeywordServiceTest {
    private final KeywordRepository keywords = mock(KeywordRepository.class);
    private final KeywordAlertService alerts = mock(KeywordAlertService.class);
    private final KeywordService service = new KeywordService(keywords, alerts);

    {
        when(keywords.save(any(Keyword.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test void addingKeywordAlertsAboutExistingMatches() {
        service.add("u-1", "  마우스 ");

        verify(alerts).notifyExistingItems("u-1", "마우스");
    }

    @Test void duplicateKeywordIsRejectedWithoutAlert() {
        when(keywords.findByRequesterUidAndKeywordIgnoreCase("u-1", "마우스")).thenReturn(Optional.of(new Keyword()));

        assertThrows(IllegalArgumentException.class, () -> service.add("u-1", "마우스"));

        verifyNoInteractions(alerts);
    }
}
