package org.example.aiguidance;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-guidance")
public class AiGuidanceController {
    private final AiGuidanceService aiGuidanceService;

    public AiGuidanceController(AiGuidanceService aiGuidanceService) {
        this.aiGuidanceService = aiGuidanceService;
    }

    @PostMapping
    public ResponseEntity<AiGuidanceResponse> generate(@Valid @RequestBody AiGuidanceRequest request) {
        return ResponseEntity.ok(aiGuidanceService.generate(request));
    }
}
