package org.example.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class S3ServiceImageTypeTest {
    private static byte[] head(int... values) {
        byte[] b = new byte[16];
        for (int i = 0; i < values.length; i++) b[i] = (byte) values[i];
        return b;
    }

    private static byte[] ascii(String s) {
        byte[] b = new byte[16];
        byte[] src = s.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(src, 0, b, 0, Math.min(src.length, 16));
        return b;
    }

    @Test void jpegIsAcceptedEvenWhenPhoneSendsNoType() {
        assertEquals("image/jpeg", S3Service.resolveImageType(head(0xFF, 0xD8, 0xFF, 0xE0), null));
        assertEquals("image/jpeg", S3Service.resolveImageType(head(0xFF, 0xD8, 0xFF, 0xE1), "application/octet-stream"));
    }

    @Test void pngWebpGifAreDetected() {
        assertEquals("image/png", S3Service.resolveImageType(head(0x89, 'P', 'N', 'G', 0x0D, 0x0A), ""));
        assertEquals("image/webp", S3Service.resolveImageType(ascii("RIFF\0\0\0\0WEBPVP8 "), null));
        assertEquals("image/gif", S3Service.resolveImageType(ascii("GIF89a"), null));
    }

    @Test void iphoneHeicIsDetected() {
        assertEquals("image/heic", S3Service.resolveImageType(ascii("\0\0\0\u0018ftypheic"), null));
    }

    @Test void webPageDisguisedAsPhotoIsStoredAsImageTypeOrRejected() {
        byte[] html = ascii("<html><script>");
        assertThrows(IllegalArgumentException.class, () -> S3Service.resolveImageType(html, "text/html"));
        assertThrows(IllegalArgumentException.class, () -> S3Service.resolveImageType(html, null));
        // 형식만 사진이라고 속여도 저장되는 Content-Type은 사진이라 브라우저가 웹페이지로 열지 않는다
        assertEquals("image/jpeg", S3Service.resolveImageType(html, "image/jpeg"));
    }
}
