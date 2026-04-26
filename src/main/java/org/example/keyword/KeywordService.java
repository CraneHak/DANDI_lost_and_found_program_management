package org.example.keyword;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class KeywordService {

    private final KeywordRepository keywordRepository;

    public KeywordService(KeywordRepository keywordRepository) {
        this.keywordRepository = keywordRepository;
    }

    public List<Keyword> findAll() {
        return keywordRepository.findAllByOrderByCreatedAtAsc();
    }

    @Transactional
    public Keyword add(String keyword) {
        Keyword entity = new Keyword();
        entity.setKeyword(keyword);
        return keywordRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!keywordRepository.existsById(id)) {
            throw new KeywordNotFoundException(id);
        }
        keywordRepository.deleteById(id);
    }
}
