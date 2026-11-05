package org.example.vision;

import java.util.List;

public record VisionAnalyzeResponse(
        Long id,
        DocumentType documentType,
        boolean ocrApplied,
        String originalImageUrl,
        String mosaicImageUrl,
        String maskedText,
        List<String> objectLabels,
        List<String> dominantColors,
        String createdAt
) {
}
