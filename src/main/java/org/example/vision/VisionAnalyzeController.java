package org.example.vision;

import org.example.common.SensitiveImagePolicy;
import org.example.report.Report;
import org.example.report.ReportRepository;
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
    private final ReportRepository reportRepository;

    public VisionAnalyzeController(VisionAnalyzeService visionAnalyzeService, ReportRepository reportRepository) {
        this.visionAnalyzeService = visionAnalyzeService;
        this.reportRepository = reportRepository;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VisionAnalyzeResponse analyze(
            @RequestParam(name = "image", required = false) MultipartFile image,
            @RequestParam(name = "reportId", required = false) Long reportId,
            @RequestParam(name = "documentType", defaultValue = "NONE") DocumentType documentType
    ) throws IOException {
        if (image != null && !image.isEmpty()) {
            return visionAnalyzeService.analyze(image, documentType);
        }
        // 사진 파일이 없으면, 검수 대기 신고에 이미 올라간 사진을 서버가 읽어서 분석한다.
        if (reportId == null) {
            throw new IllegalArgumentException("Image file is required.");
        }
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다. id=" + reportId));
        String stored = report.getImage();
        if (stored == null || stored.isBlank()) {
            throw new IllegalArgumentException("이 신고에는 사진이 없습니다.");
        }
        if (SensitiveImagePolicy.isMosaic(stored)) {
            throw new IllegalArgumentException("이미 모자이크가 적용된 사진입니다.");
        }
        return visionAnalyzeService.analyzeStoredImage(stored, documentType);
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
