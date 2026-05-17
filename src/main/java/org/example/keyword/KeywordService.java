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

    public KeywordService(KeywordRepository keywordRepository) {
        this.keywordRepository = keywordRepository;
    }

    public List<Keyword> findAll() {
        return keywordRepository.findAllByOrderByCreatedAtAsc();
    }

    @Transactional
    public Keyword add(String keyword) {
        String normalized = normalizeKeyword(keyword);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("keyword is required.");
        }
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("keyword must be at most 30 characters.");
        }
        if (keywordRepository.findByKeywordIgnoreCase(normalized).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 키워드입니다.");
        }
        if (keywordRepository.count() >= MAX_KEYWORD_COUNT) {
            throw new IllegalArgumentException("키워드는 최대 10개까지 등록할 수 있습니다.");
        }

        Keyword entity = new Keyword();
        entity.setKeyword(normalized);
        return keywordRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!keywordRepository.existsById(id)) {
            throw new KeywordNotFoundException(id);
        }
        keywordRepository.deleteById(id);
    }

    private String normalizeKeyword(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ");
    }
}
