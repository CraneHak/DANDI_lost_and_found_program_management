package org.example.vision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VisionAnalyzeService {
    private final VisionClient visionClient;
    private final VisionMaskingService visionMaskingService;
    private final ImageMosaicService imageMosaicService;
    private final VisionImageStore visionImageStore;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<LostItemAnalysisRepository> repositoryProvider;

    public VisionAnalyzeService(
            VisionClient visionClient,
            VisionMaskingService visionMaskingService,
            ImageMosaicService imageMosaicService,
            VisionImageStore visionImageStore,
            ObjectMapper objectMapper,
            ObjectProvider<LostItemAnalysisRepository> repositoryProvider
    ) {
        this.visionClient = visionClient;
        this.visionMaskingService = visionMaskingService;
        this.imageMosaicService = imageMosaicService;
        this.visionImageStore = visionImageStore;
        this.objectMapper = objectMapper;
        this.repositoryProvider = repositoryProvider;
    }

    public VisionAnalyzeResponse analyze(MultipartFile image, DocumentType documentType) throws IOException {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file is required.");
        }
        if (!isImageContentType(image.getContentType())) {
            throw new IllegalArgumentException("Only image files are allowed.");
        }

        byte[] imageBytes = image.getBytes();
        String extension = detectExtension(image);

        VisionAnalysisResult analysisResult = visionClient.analyze(imageBytes, documentType.requiresOcr());
        MaskedOcrResult maskedOcrResult = visionMaskingService.apply(documentType, analysisResult);

        String originalPath = visionImageStore.saveOriginal(imageBytes, extension);
        String mosaicPath = null;
        if (documentType.requiresOcr() && !maskedOcrResult.sensitiveBoxes().isEmpty()) {
            byte[] mosaicBytes = imageMosaicService.mosaic(imageBytes, maskedOcrResult.sensitiveBoxes(), extension);
            mosaicPath = visionImageStore.saveMosaic(mosaicBytes, extension);
        }

        LostItemAnalysisRecord saved = saveRecord(documentType, analysisResult, maskedOcrResult, originalPath, mosaicPath)
                .orElse(null);

        return new VisionAnalyzeResponse(
                saved != null ? saved.getId() : null,
                documentType,
                documentType.requiresOcr(),
                visionImageStore.toPublicUrl(originalPath),
                mosaicPath != null ? visionImageStore.toPublicUrl(mosaicPath) : null,
                maskedOcrResult.maskedText(),
                analysisResult.objectLabels(),
                analysisResult.dominantColors(),
                saved != null ? saved.getCreatedAt() : LocalDateTime.now()
        );
    }

    public VisionAnalyzeResponse getResult(Long id) {
        LostItemAnalysisRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new IllegalStateException("Database is not enabled in this environment.");
        }
        LostItemAnalysisRecord record = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Result not found: " + id));
        return toResponse(record);
    }

    public List<VisionAnalyzeResponse> getRecentResults() {
        LostItemAnalysisRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) {
            return List.of();
        }
        return repository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private Optional<LostItemAnalysisRecord> saveRecord(
            DocumentType documentType,
            VisionAnalysisResult analysisResult,
            MaskedOcrResult maskedOcrResult,
            String originalPath,
            String mosaicPath
    ) {
        LostItemAnalysisRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) {
            return Optional.empty();
        }

        LostItemAnalysisRecord record = new LostItemAnalysisRecord();
        record.setDocumentType(documentType);
        record.setOcrApplied(documentType.requiresOcr());
        record.setOriginalImagePath(originalPath);
        record.setMosaicImagePath(mosaicPath);
        record.setMaskedText(maskedOcrResult.maskedText());
        record.setObjectLabelsJson(toJson(analysisResult.objectLabels()));
        record.setDominantColorsJson(toJson(analysisResult.dominantColors()));
        return Optional.of(repository.save(record));
    }

    private VisionAnalyzeResponse toResponse(LostItemAnalysisRecord record) {
        return new VisionAnalyzeResponse(
                record.getId(),
                record.getDocumentType(),
                record.isOcrApplied(),
                visionImageStore.toPublicUrl(record.getOriginalImagePath()),
                record.getMosaicImagePath() != null ? visionImageStore.toPublicUrl(record.getMosaicImagePath()) : null,
                record.getMaskedText(),
                fromJson(record.getObjectLabelsJson()),
                fromJson(record.getDominantColorsJson()),
                record.getCreatedAt()
        );
    }

    private String detectExtension(MultipartFile image) {
        String contentType = image.getContentType();
        if (MediaType.IMAGE_JPEG_VALUE.equals(contentType) || "image/jpg".equals(contentType)) {
            return "jpeg";
        }
        if (MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
            return "png";
        }
        if ("image/bmp".equals(contentType)) {
            return "bmp";
        }

        String originalFilename = image.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(originalFilename);
        if (ext == null) {
            return "png";
        }
        return ext.toLowerCase();
    }

    private boolean isImageContentType(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    private String toJson(List<String> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize result.", e);
        }
    }

    private List<String> fromJson(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
