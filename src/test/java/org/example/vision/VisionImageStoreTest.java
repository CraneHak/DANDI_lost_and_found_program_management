package org.example.vision;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VisionImageStoreTest {
    private final VisionImageStore store = new VisionImageStore();

    @Test void onlyMosaicPathsGetPublicUrls() {
        assertEquals("/files/vision/mosaic/a.png", store.toPublicUrl("vision/mosaic/a.png"));
        assertEquals("/files/vision/mosaic/a.png", store.toPublicUrl("vision\\mosaic\\a.png"));
        assertNull(store.toPublicUrl("vision/original/a.png"));
        assertNull(store.toPublicUrl(""));
        assertNull(store.toPublicUrl(null));
    }
}
