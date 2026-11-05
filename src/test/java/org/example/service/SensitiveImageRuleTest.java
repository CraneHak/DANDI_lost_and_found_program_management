package org.example.service;

import org.example.vision.VisionImageStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveImageRuleTest {

    @Test
    void idAndCardItemsAreSensitive() {
        assertTrue(LostItemService.isSensitiveItem("신분증", null));
        assertTrue(LostItemService.isSensitiveItem(null, "단국대 학생증"));
        assertTrue(LostItemService.isSensitiveItem("카드/티켓", "교통카드"));
        assertFalse(LostItemService.isSensitiveItem("전자기기", "에어팟"));
    }

    @Test
    void onlyMosaicUrlsCountAsMasked() {
        assertTrue(LostItemService.isMosaicImage("/files/vision/mosaic/a.png"));
        assertTrue(LostItemService.isMosaicImage("https://api.dandilost.life/files/vision/mosaic/a.png"));
        assertFalse(LostItemService.isMosaicImage("https://dandi-lost-items.s3.ap-northeast-2.amazonaws.com/lost-items/a.jpg"));
        assertFalse(LostItemService.isMosaicImage("/files/vision/original/a.png"));
        assertFalse(LostItemService.isMosaicImage(null));
    }

    @Test
    void visionStoreNeverBuildsOriginalUrls() {
        VisionImageStore store = new VisionImageStore();
        assertEquals("/files/vision/mosaic/a.png", store.toPublicUrl("vision/mosaic/a.png"));
        assertNull(store.toPublicUrl("vision/original/a.png"));
        assertNull(store.toPublicUrl(""));
        assertNull(store.toPublicUrl(null));
    }
}
