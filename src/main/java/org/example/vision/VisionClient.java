package org.example.vision;

import java.io.IOException;

public interface VisionClient {
    VisionAnalysisResult analyze(byte[] imageBytes, boolean includeOcr) throws IOException;
}
