package org.example.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveImagePolicyTest {

    private static final String MOSAIC = "/files/vision/mosaic/643f6f09.jpeg";
    private static final String RAW = "https://dandi-lost-items.s3.ap-northeast-2.amazonaws.com/upload-1.jpg";

    @Test void idCardWithRawPhotoIsRejectedWithConflict() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> SensitiveImagePolicy.requireMasked("신분증", RAW));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(SensitiveImagePolicy.MESSAGE, ex.getReason());
    }

    @Test void cardWithRawPhotoIsRejected() {
        assertThrows(ResponseStatusException.class, () -> SensitiveImagePolicy.requireMasked("카드/티켓", RAW));
        assertThrows(ResponseStatusException.class, () -> SensitiveImagePolicy.requireMasked("카드", RAW));
    }

    @Test void dataUrlPhotoIsRejectedForSensitiveType() {
        assertThrows(ResponseStatusException.class,
                () -> SensitiveImagePolicy.requireMasked("신분증", "data:image/png;base64,AAAA"));
    }

    @Test void mosaicPhotoIsAllowed() {
        assertDoesNotThrow(() -> SensitiveImagePolicy.requireMasked("신분증", MOSAIC));
        assertDoesNotThrow(() -> SensitiveImagePolicy.requireMasked("카드/티켓",
                "https://api.dandilost.life" + MOSAIC));
    }

    @Test void ordinaryItemWithRawPhotoIsAllowed() {
        assertDoesNotThrow(() -> SensitiveImagePolicy.requireMasked("화장품", RAW));
        assertDoesNotThrow(() -> SensitiveImagePolicy.requireMasked("전자기기", RAW));
        assertDoesNotThrow(() -> SensitiveImagePolicy.requireMasked(null, RAW));
    }

    @Test void walletWithCardInNameIsNotSensitive() {
        assertFalse(SensitiveImagePolicy.isSensitive("카드지갑"));
        assertFalse(SensitiveImagePolicy.isSensitive("지갑/가방"));
        assertDoesNotThrow(() -> SensitiveImagePolicy.requireMasked("카드지갑", RAW));
    }

    @Test void noPhotoIsAllowedEvenForSensitiveType() {
        assertDoesNotThrow(() -> SensitiveImagePolicy.requireMasked("신분증", null));
        assertDoesNotThrow(() -> SensitiveImagePolicy.requireMasked("신분증", "  "));
    }

    @Test void sensitiveTypeAmongSeveralIsDetected() {
        assertTrue(SensitiveImagePolicy.isSensitive("기타, 신분증"));
        assertThrows(ResponseStatusException.class, () -> SensitiveImagePolicy.requireMasked("기타,신분증", RAW));
    }

    @Test void rawUploadIsRejectedOnlyForSensitiveType() {
        assertThrows(ResponseStatusException.class, () -> SensitiveImagePolicy.rejectRawUpload("카드/티켓"));
        assertDoesNotThrow(() -> SensitiveImagePolicy.rejectRawUpload("화장품"));
        assertDoesNotThrow(() -> SensitiveImagePolicy.rejectRawUpload(null));
    }

    @Test void sameImageIgnoresHostAndScheme() {
        assertTrue(SensitiveImagePolicy.sameImage("/files/vision/mosaic/a.jpg",
                "https://api.dandilost.life/files/vision/mosaic/a.jpg"));
        assertFalse(SensitiveImagePolicy.sameImage("/files/a.jpg", "/files/b.jpg"));
        assertFalse(SensitiveImagePolicy.sameImage(null, "/files/a.jpg"));
    }
}
