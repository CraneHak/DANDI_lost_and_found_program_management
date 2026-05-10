package org.example.aiguidance;

import jakarta.validation.constraints.NotBlank;

public record AiGuidanceRequest(
        @NotBlank(message = "productName is required")
        String productName,
        String productCategory,
        String productDescription
) {
}
