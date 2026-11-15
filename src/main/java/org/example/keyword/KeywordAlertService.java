package org.example.keyword;

import org.example.auth.UserProfile;
import org.example.auth.UserProfileRepository;
import org.example.entity.ItemStatus;
import org.example.entity.LostItem;
import org.example.notice.NoticeService;
import org.example.repository.LostItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
public class KeywordAlertService {

    private static final String ALERT_TITLE = "관심 키워드 알림";
    /** 키워드를 새로 등록했을 때 알림에 이름을 나열하는 물품 수 (나머지는 "외 N건"으로 줄인다) */
    private static final int MAX_LISTED_ITEMS = 5;

    private final KeywordRepository keywordRepository;
    private final NoticeService noticeService;
    private final UserProfileRepository userProfileRepository;
    private final LostItemRepository lostItemRepository;

    public KeywordAlertService(
            KeywordRepository keywordRepository,
            NoticeService noticeService,
            UserProfileRepository userProfileRepository,
            LostItemRepository lostItemRepository
    ) {
        this.keywordRepository = keywordRepository;
        this.noticeService = noticeService;
        this.userProfileRepository = userProfileRepository;
        this.lostItemRepository = lostItemRepository;
    }

    /** 새 물품이 공개될 때: 그 물품과 일치하는 키워드를 가진 사용자(관리자 포함)에게 알린다. */
    @Transactional
    public void notifyMatchingUsers(LostItem item) {
        if (item == null || item.getStatus() != ItemStatus.ACQUIRED) {
            return;
        }
        String haystack = buildHaystack(item);
        if (haystack.isBlank()) {
            return;
        }

        List<Keyword> keywords = keywordRepository.findAll();
        Map<String, Boolean> alertEnabledByUid = new HashMap<>();
        for (Keyword keyword : keywords) {
            String term = keyword.getKeyword();
            if (term == null || term.isBlank()) {
                continue;
            }
            if (!containsIgnoreCase(haystack, term)) {
                continue;
            }
            String uid = keyword.getRequesterUid();
            if (uid == null || uid.isBlank()) {
                continue;
            }
            // 마이페이지에서 '매칭 알림 받기'를 끈 사용자는 건너뛴다 (프로필이 없으면 기본값 켜짐)
            if (!alertEnabledByUid.computeIfAbsent(uid, this::isAlertEnabled)) {
                continue;
            }
            noticeService.createKeywordAlert(
                    uid,
                    ALERT_TITLE,
                    "설정하신 관심 키워드와 일치하는 습득물이 등록되었습니다.\n\n"
                            + "키워드: " + term.trim() + "\n"
                            + "물품: " + safe(item.getItemName()) + "\n"
                            + "위치: " + safe(item.getFoundLocation())
            );
        }
    }

    /**
     * 키워드를 새로 등록했을 때: 이미 공개돼 있는(수령 가능한) 물품 중 일치하는 것을 알림 1건으로 모아 보낸다.
     * 한 번에 알림이 쏟아지지 않도록 물품 수와 상관없이 알림은 1건이다.
     */
    @Transactional
    public void notifyExistingItems(String uid, String term) {
        if (uid == null || uid.isBlank() || term == null || term.isBlank()) {
            return;
        }
        if (!isAlertEnabled(uid)) {
            return;
        }
        List<LostItem> matches = lostItemRepository.findByStatusOrderByIdDesc(ItemStatus.ACQUIRED).stream()
                .filter(item -> containsIgnoreCase(buildHaystack(item), term))
                .toList();
        if (matches.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder()
                .append("등록하신 키워드와 일치하는 습득물이 ").append(matches.size()).append("건 있습니다.\n\n")
                .append("키워드: ").append(term.trim()).append("\n");
        matches.stream().limit(MAX_LISTED_ITEMS).forEach(item -> message
                .append("- ").append(safe(item.getItemName()))
                .append(" (").append(safe(item.getFoundLocation())).append(")\n"));
        if (matches.size() > MAX_LISTED_ITEMS) {
            message.append("외 ").append(matches.size() - MAX_LISTED_ITEMS)
                    .append("건은 홈에서 키워드로 검색해 확인해 주세요.");
        }
        noticeService.createKeywordAlert(uid, ALERT_TITLE, message.toString().stripTrailing());
    }

    // 프로필이 없으면 기본값 켜짐
    private boolean isAlertEnabled(String uid) {
        return userProfileRepository.findByFirebaseUid(uid)
                .map(UserProfile::isKeywordAlertEnabled)
                .orElse(true);
    }

    private String buildHaystack(LostItem item) {
        return String.join(
                " ",
                safe(item.getItemName()),
                safe(item.getItemType()),
                safe(item.getFoundLocation()),
                safe(item.getLostLocation()),
                safe(item.getStoredLocation()),
                safe(item.getContact())
        ).toLowerCase(Locale.ROOT);
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack.contains(needle.trim().toLowerCase(Locale.ROOT));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
