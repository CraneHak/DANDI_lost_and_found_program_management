package org.example.vision;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class ImageMosaicService {
    public byte[] mosaic(byte[] imageBytes, List<OcrBox> boxes, String format) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (source == null) {
            throw new IOException("Failed to decode image.");
        }
        BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();

        for (OcrBox box : boxes) {
            applyRedaction(target, box.left(), box.top(), box.right(), box.bottom());
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String normalizedFormat = normalizeFormat(format);
        ImageIO.write(target, normalizedFormat, outputStream);
        return outputStream.toByteArray();
    }

    private void applyRedaction(BufferedImage image, int left, int top, int right, int bottom) {
        int startX = Math.max(0, left);
        int startY = Math.max(0, top);
        int endX = Math.min(image.getWidth(), right);
        int endY = Math.min(image.getHeight(), bottom);
        if (startX >= endX || startY >= endY) {
            return;
        }
        int black = Color.BLACK.getRGB();
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                image.setRGB(x, y, black);
            }
        }
    }

    private String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            return "png";
        }
        String lower = format.toLowerCase();
        return switch (lower) {
            case "jpg", "jpeg", "png", "bmp" -> lower.equals("jpg") ? "jpeg" : lower;
            default -> "png";
        };
    }
}
