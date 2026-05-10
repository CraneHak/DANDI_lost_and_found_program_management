package org.example.chatbot.policy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.auth.FirebaseAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class ChatbotRateLimitInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ChatbotRateLimitInterceptor.class);
    private final ChatbotRateLimitService rateLimitService;

    public ChatbotRateLimitInterceptor(ChatbotRateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uid = resolveUid();
        if (uid == null || uid.isBlank()) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Unauthorized.");
            return false;
        }

        RateLimitDecision decision = rateLimitService.evaluateAndConsume(uid, Instant.now());
        String endpoint = request.getRequestURI();
        String clientIp = extractClientIp(request);

        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            log.warn("chatbot.rate_limit.blocked uid={} ip={} endpoint={} retry_after={} reason={}",
                    uid, clientIp, endpoint, decision.retryAfterSeconds(), decision.reason());
            writeError(response, HttpStatus.TOO_MANY_REQUESTS, decision.reason());
            return false;
        }

        log.info("chatbot.rate_limit.allowed uid={} ip={} endpoint={} remaining(min/hour/day)={}/{}/{}",
                uid, clientIp, endpoint, decision.minuteRemaining(), decision.hourRemaining(), decision.dayRemaining());
        return true;
    }

    private String resolveUid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof FirebaseAuthenticationToken firebaseToken) {
            return firebaseToken.getUid();
        }
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        return principal != null ? principal.toString() : null;
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int comma = forwardedFor.indexOf(',');
            return comma >= 0 ? forwardedFor.substring(0, comma).trim() : forwardedFor.trim();
        }
        return request.getRemoteAddr();
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
