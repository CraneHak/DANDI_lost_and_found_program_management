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

    public String saveOriginal(byte[] bytes, String extension) throws IOException {
        return save(bytes, "original", extension);
    }

    public String saveMosaic(byte[] bytes, String extension) throws IOException {
        return save(bytes, "mosaic", extension);
    }

    public String toPublicUrl(String relativePath) {
        return "/files/" + relativePath.replace('\\', '/');
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
