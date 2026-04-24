package org.example.vision;

import java.util.List;

public record MaskedOcrResult(
        String maskedText,
        List<OcrBox> sensitiveBoxes
) {
}
