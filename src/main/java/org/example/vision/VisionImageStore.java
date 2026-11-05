package org.example.vision;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Component
public class VisionImageStore {
    private final Path root = Path.of("uploads", "vision");

    public String saveMosaic(byte[] bytes, String extension) throws IOException {
        return save(bytes, "mosaic", extension);
    }

    /** 모자이크만 공개 경로로 서빙된다. 예전 원본 경로나 빈 값은 URL을 만들지 않는다. */
    public String toPublicUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String normalized = relativePath.replace('\\', '/');
        if (!normalized.startsWith("vision/mosaic/")) {
            return null;
        }
        return "/files/" + normalized;
    }

    private String save(byte[] bytes, String folder, String extension) throws IOException {
        String normalizedExtension = normalizeExtension(extension);
        Path directory = root.resolve(folder);
        Files.createDirectories(directory);

        String fileName = UUID.randomUUID() + "." + normalizedExtension;
        Path target = directory.resolve(fileName);
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        return Path.of("vision", folder, fileName).toString();
    }

    private String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "png";
        }
        String lower = extension.toLowerCase();
        return switch (lower) {
            case "jpg", "jpeg", "png", "bmp" -> lower.equals("jpg") ? "jpeg" : lower;
            default -> "png";
        };
    }
}
