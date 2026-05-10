package org.example.chatbot;

import jakarta.validation.constraints.NotBlank;

public record ChatbotRequest(
        @NotBlank(message = "message is required")
        String message
) {
}
