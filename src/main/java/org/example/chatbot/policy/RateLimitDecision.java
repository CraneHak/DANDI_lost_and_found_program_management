package org.example.chatbot.policy;

public record RateLimitDecision(
        boolean allowed,
        int minuteRemaining,
        int hourRemaining,
        int dayRemaining,
        long retryAfterSeconds,
        String reason
) {
}
