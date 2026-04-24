package org.example.vision;

import java.util.List;

public record VisionAnalysisResult(
        List<String> objectLabels,
        List<String> dominantColors,
        String rawText,
        List<OcrBox> ocrBoxes
) {
}
