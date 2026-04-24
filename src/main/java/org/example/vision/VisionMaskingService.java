package org.example.vision;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VisionMaskingService {
    private static final Pattern RRN_PATTERN = Pattern.compile("(\\d{6})\\s*[- ]?\\s*(\\d{7})");
    private static final Pattern STUDENT_NO_PATTERN = Pattern.compile("\\b\\d{8,10}\\b");
    private static final Pattern CARD_NO_PATTERN = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");
    private static final Pattern EXPIRY_PATTERN = Pattern.compile("\\b(0[1-9]|1[0-2])[/-]?\\d{2}\\b");
    private static final Pattern CARD_HOLDER_PATTERN = Pattern.compile("\\b[A-Z]{2,}(?:\\s+[A-Z]{2,})+\\b");
    private static final Pattern KOREAN_NAME_PATTERN = Pattern.compile("(?<![\\uAC00-\\uD7A3])[\\uAC00-\\uD7A3]{2,4}(?![\\uAC00-\\uD7A3])");
    private static final Pattern HANJA_NAME_PATTERN = Pattern.compile("(?<![\\u4E00-\\u9FFF])[\\u4E00-\\u9FFF]{2,4}(?![\\u4E00-\\u9FFF])");
    private static final Pattern ADDRESS_NUMBER_PATTERN = Pattern.compile("\\b\\d{1,4}(?:-\\d{1,4})?\\b");
    private static final Pattern PAREN_TOKEN_PATTERN = Pattern.compile("[()\\uFF08\\uFF09]");

    private static final String RESIDENT_ID_CARD = "\uC8FC\uBBFC\uB4F1\uB85D\uC99D";
    private static final String LABEL_NAME = "\uC774\uB984";
    private static final String LABEL_RRN = "\uC8FC\uBBFC\uBC88\uD638";

    private static final Set<String> NON_NAME_KOREAN_WORDS = Set.of(
            "\uB300\uD55C\uBBFC\uAD6D",
            "\uC8FC\uBBFC\uB4F1\uB85D\uC99D",
            "\uC131\uBA85",
            "\uC8FC\uC18C",
            "\uBC1C\uAE09\uC77C",
            "\uC8FC\uBBFC",
            "\uB4F1\uB85D",
            "\uC99D\uBA85"
    );

    public MaskedOcrResult apply(DocumentType documentType, VisionAnalysisResult analysisResult) {
        if (!documentType.requiresOcr()) {
            return new MaskedOcrResult("", List.of());
        }

        List<OcrBox> sensitiveBoxes = new ArrayList<>();
        for (OcrBox box : analysisResult.ocrBoxes()) {
            String text = box.text();
            if (text != null && !text.isBlank()) {
                sensitiveBoxes.add(expandForMosaic(box, documentType));
            }
        }

        String maskedText = switch (documentType) {
            case ID_CARD -> extractIdCardMaskedFields(analysisResult);
            case STUDENT_ID, BANK_CARD -> buildMaskedTextByLine(documentType, analysisResult);
            case NONE -> "";
        };

        return new MaskedOcrResult(maskedText, sensitiveBoxes);
    }

    private OcrBox expandForMosaic(OcrBox box, DocumentType documentType) {
        int width = Math.max(1, box.right() - box.left());
        int height = Math.max(1, box.bottom() - box.top());

        int marginX = Math.max(8, (int) (width * 0.12));
        int marginY = Math.max(6, (int) (height * 0.20));

        if (documentType == DocumentType.ID_CARD) {
            marginX += 10;
            marginY += 8;
            if (box.text() != null && PAREN_TOKEN_PATTERN.matcher(box.text()).find()) {
                marginX += Math.max(16, (int) (width * 0.35));
                marginY += 4;
            }
        }

        return new OcrBox(
                box.left() - marginX,
                box.top() - marginY,
                box.right() + marginX,
                box.bottom() + marginY,
                box.text()
        );
    }

    private String extractIdCardMaskedFields(VisionAnalysisResult analysisResult) {
        String raw = normalizeText(analysisResult.rawText());

        String name = extractIdCardName(raw);
        if (name.isBlank()) {
            name = fallbackNameFromBoxes(analysisResult.ocrBoxes());
        }

        String rrn = extractRrn(raw);
        if (rrn.isBlank()) {
            rrn = extractRrnFromBoxes(analysisResult.ocrBoxes());
        }

        String maskedName = name.isBlank() ? "" : maskPersonName(name);
        String maskedRrn = rrn.isBlank() ? "" : "******-*******";

        StringBuilder builder = new StringBuilder();
        if (!maskedName.isBlank()) {
            builder.append(LABEL_NAME).append(": ").append(maskedName);
        }
        if (!maskedRrn.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(LABEL_RRN).append(": ").append(maskedRrn);
        }
        return builder.toString();
    }

    private String extractIdCardName(String raw) {
        int cardIdx = raw.indexOf(RESIDENT_ID_CARD);
        if (cardIdx < 0) {
            return "";
        }
        String afterCard = raw.substring(cardIdx + RESIDENT_ID_CARD.length()).trim();
        int openParenIdx = indexOfAny(afterCard, '(', '\uFF08');
        String candidate = openParenIdx >= 0 ? afterCard.substring(0, openParenIdx) : afterCard;
        candidate = candidate.replaceAll("\\s+", "");
        candidate = candidate.replaceAll("[^\\uAC00-\\uD7A3\\u4E00-\\u9FFF]", "");
        if (candidate.length() < 2 || candidate.length() > 6) {
            return "";
        }
        return candidate;
    }

    private String fallbackNameFromBoxes(List<OcrBox> boxes) {
        for (OcrBox box : boxes) {
            String text = box.text();
            if (text == null) {
                continue;
            }
            String normalized = text.replaceAll("\\s+", "");
            if (normalized.isBlank()) {
                continue;
            }
            if (NON_NAME_KOREAN_WORDS.contains(normalized)) {
                continue;
            }
            if (KOREAN_NAME_PATTERN.matcher(normalized).matches() || HANJA_NAME_PATTERN.matcher(normalized).matches()) {
                return normalized;
            }
        }
        return "";
    }

    private String extractRrn(String raw) {
        int closeParenIdx = indexOfAny(raw, ')', '\uFF09');
        String target = closeParenIdx >= 0 ? raw.substring(closeParenIdx + 1) : raw;
        Matcher matcher = RRN_PATTERN.matcher(target);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1) + matcher.group(2);
    }

    private String extractRrnFromBoxes(List<OcrBox> boxes) {
        for (OcrBox box : boxes) {
            String text = box.text();
            if (text == null || text.isBlank()) {
                continue;
            }
            Matcher matcher = RRN_PATTERN.matcher(text);
            if (matcher.find()) {
                return matcher.group(1) + matcher.group(2);
            }
        }
        return "";
    }

    private String buildMaskedTextByLine(DocumentType type, VisionAnalysisResult analysisResult) {
        StringBuilder builder = new StringBuilder();
        for (OcrBox box : analysisResult.ocrBoxes()) {
            String masked = maskByType(type, box.text());
            if (masked.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(masked);
        }
        if (!builder.isEmpty()) {
            return builder.toString();
        }
        return maskByType(type, analysisResult.rawText());
    }

    private String maskByType(DocumentType type, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return switch (type) {
            case ID_CARD -> maskIdCard(value);
            case STUDENT_ID -> maskStudentId(value);
            case BANK_CARD -> maskBankCard(value);
            case NONE -> value;
        };
    }

    private String maskIdCard(String input) {
        String masked = RRN_PATTERN.matcher(input).replaceAll("******-*******");
        if (containsAddressToken(masked)) {
            masked = maskAddressNumbers(masked);
        }
        return maskKoreanAndHanjaNames(masked);
    }

    private String maskStudentId(String input) {
        String masked = maskKoreanAndHanjaNames(input);
        Matcher matcher = STUDENT_NO_PATTERN.matcher(masked);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String number = matcher.group();
            String replacement = number.substring(0, 2) + "*".repeat(Math.max(0, number.length() - 2));
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String maskBankCard(String input) {
        String masked = CARD_NO_PATTERN.matcher(input).replaceAll(match -> maskCardNumber(match.group()));
        masked = EXPIRY_PATTERN.matcher(masked).replaceAll("**/**");
        masked = CARD_HOLDER_PATTERN.matcher(masked).replaceAll(match -> maskEnglishName(match.group()));
        return maskKoreanAndHanjaNames(masked);
    }

    private String maskCardNumber(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() < 8) {
            return raw;
        }
        String tail = digits.substring(digits.length() - 4);
        return "**** **** **** " + tail;
    }

    private String maskEnglishName(String name) {
        String[] parts = name.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            String maskedPart = part.charAt(0) + "*".repeat(Math.max(1, part.length() - 1));
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(maskedPart);
        }
        return builder.toString();
    }

    private String maskKoreanAndHanjaNames(String input) {
        String masked = maskNamePattern(input, KOREAN_NAME_PATTERN, true);
        return maskNamePattern(masked, HANJA_NAME_PATTERN, false);
    }

    private String maskNamePattern(String input, Pattern pattern, boolean skipCommonKoreanWords) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group();
            if (containsAddressToken(token)) {
                continue;
            }
            if (skipCommonKoreanWords && NON_NAME_KOREAN_WORDS.contains(token)) {
                continue;
            }
            matcher.appendReplacement(buffer, maskPersonName(token));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String maskPersonName(String value) {
        if (value.length() == 2) {
            return value.charAt(0) + "*";
        }
        return value.charAt(0) + "*".repeat(value.length() - 2) + value.charAt(value.length() - 1);
    }

    private String maskAddressNumbers(String input) {
        Matcher matcher = ADDRESS_NUMBER_PATTERN.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String number = matcher.group();
            matcher.appendReplacement(buffer, "*".repeat(number.length()));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private boolean containsAddressToken(String value) {
        return value.contains("\uC2DC")
                || value.contains("\uB3C4")
                || value.contains("\uAD70")
                || value.contains("\uAD6C")
                || value.contains("\uC74D")
                || value.contains("\uBA74")
                || value.contains("\uB3D9")
                || value.contains("\uB9AC")
                || value.contains("\uB85C")
                || value.contains("\uAE38");
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private int indexOfAny(String value, char first, char second) {
        int idx1 = value.indexOf(first);
        int idx2 = value.indexOf(second);
        if (idx1 < 0) {
            return idx2;
        }
        if (idx2 < 0) {
            return idx1;
        }
        return Math.min(idx1, idx2);
    }
}
