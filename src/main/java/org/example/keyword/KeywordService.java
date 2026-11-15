package org.example.keyword;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class KeywordService {
    private static final int MAX_KEYWORD_COUNT = 10;
    private static final int MAX_KEYWORD_LENGTH = 30;

    private final KeywordRepository keywordRepository;
    private final KeywordAlertService keywordAlertService;

    public KeywordService(KeywordRepository keywordRepository, KeywordAlertService keywordAlertService) {
        this.keywordRepository = keywordRepository;
        this.keywordAlertService = keywordAlertService;
    }

    public List<Keyword> findAll(String requesterUid) {
        return keywordRepository.findAllByRequesterUidOrderByCreatedAtAsc(requesterUid);
    }

    @Transactional
    public Keyword add(String requesterUid, String keyword) {
        String normalized = normalizeKeyword(keyword);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("keyword is required.");
        }
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("keyword must be at most 30 characters.");
        }
        if (keywordRepository.findByRequesterUidAndKeywordIgnoreCase(requesterUid, normalized).isPresent()) {
            throw new IllegalArgumentException("keyword already exists.");
        }
        if (keywordRepository.countByRequesterUid(requesterUid) >= MAX_KEYWORD_COUNT) {
            throw new IllegalArgumentException("you can register up to 10 keywords.");
        }

        Keyword entity = new Keyword();
        entity.setKeyword(normalized);
        entity.setRequesterUid(requesterUid);
        Keyword saved = keywordRepository.save(entity);
        // 이미 공개돼 있는 물품 중 새 키워드와 일치하는 것도 알려 준다 (새로 공개될 때만 알림이 가던 빈틈)
        keywordAlertService.notifyExistingItems(requesterUid, normalized);
        return saved;
    }

    @Transactional
    public void delete(String requesterUid, Long id) {
        if (keywordRepository.findByIdAndRequesterUid(id, requesterUid).isEmpty()) {
            throw new KeywordNotFoundException(id);
        }
        keywordRepository.deleteById(id);
    }

    private String normalizeKeyword(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ");
    }
}
