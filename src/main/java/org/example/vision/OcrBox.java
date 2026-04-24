package org.example.vision;

public record OcrBox(
        int left,
        int top,
        int right,
        int bottom,
        String text
) {
}
