package org.example.vision;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/vision")
public class VisionAnalyzeController {
    private final VisionAnalyzeService visionAnalyzeService;

    public VisionAnalyzeController(VisionAnalyzeService visionAnalyzeService) {
        this.visionAnalyzeService = visionAnalyzeService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VisionAnalyzeResponse analyze(
            @RequestParam("image") MultipartFile image,
            @RequestParam(name = "documentType", defaultValue = "NONE") DocumentType documentType
    ) throws IOException {
        return visionAnalyzeService.analyze(image, documentType);
    }

    @GetMapping("/results/{id}")
    public VisionAnalyzeResponse getResult(@PathVariable Long id) {
        return visionAnalyzeService.getResult(id);
    }

    @GetMapping("/results")
    public List<VisionAnalyzeResponse> getRecentResults() {
        return visionAnalyzeService.getRecentResults();
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, IOException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(Exception exception) {
        if (exception instanceof IOException || exception instanceof IllegalStateException) {
            return ResponseEntity.internalServerError().body(Map.of("message", exception.getMessage()));
        }
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
