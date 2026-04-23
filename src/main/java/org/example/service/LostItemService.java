package org.example.service;

import org.example.entity.LostItem;
import org.example.repository.LostItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class LostItemService {

    private final LostItemRepository repository;
    private final S3Service s3Service;

    public LostItemService(LostItemRepository repository, S3Service s3Service) {
        this.repository = repository;
        this.s3Service = s3Service;
    }

    public List<LostItem> findAll() {
        return repository.findAll();
    }

    public LostItem findById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("분실물을 찾을 수 없습니다. id=" + id));
    }

    public LostItem save(LostItem lostItem, MultipartFile image) throws IOException {
        if (image != null && !image.isEmpty()) {
            String imageUrl = s3Service.upload(image);
            lostItem.setImageUrl(imageUrl);
        }
        return repository.save(lostItem);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
