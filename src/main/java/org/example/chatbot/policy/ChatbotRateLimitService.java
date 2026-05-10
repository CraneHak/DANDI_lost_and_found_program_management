package org.example.chatbot.policy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatbotRateLimitService {
    private static final Duration MINUTE_WINDOW = Duration.ofMinutes(1);
    private static final Duration HOUR_WINDOW = Duration.ofHours(1);
    private static final Duration DAY_WINDOW = Duration.ofDays(1);
    private static final Duration STRIKE_WINDOW = Duration.ofDays(30);

    private final int minuteLimit;
    private final int hourLimit;
    private final int dayLimit;
    private final Duration firstBlockDuration;
    private final Duration secondBlockDuration;

    private final Map<String, UserUsage> usageByUid = new ConcurrentHashMap<>();
    private final Map<String, SanctionState> sanctionsByUid = new ConcurrentHashMap<>();

    public ChatbotRateLimitService(
            @Value("${chatbot.rate-limit.minute:12}") int minuteLimit,
            @Value("${chatbot.rate-limit.hour:20}") int hourLimit,
            @Value("${chatbot.rate-limit.day:30}") int dayLimit,
            @Value("${chatbot.sanction.first-block-minutes:15}") long firstBlockMinutes,
            @Value("${chatbot.sanction.second-block-hours:24}") long secondBlockHours
    ) {
        this.minuteLimit = minuteLimit;
        this.hourLimit = hourLimit;
        this.dayLimit = dayLimit;
        this.firstBlockDuration = Duration.ofMinutes(firstBlockMinutes);
        this.secondBlockDuration = Duration.ofHours(secondBlockHours);
    }

    public RateLimitDecision evaluateAndConsume(String uid, Instant now) {
        SanctionState sanction = sanctionsByUid.computeIfAbsent(uid, key -> new SanctionState());
        if (sanction.blockedUntil != null && now.isBefore(sanction.blockedUntil)) {
            long retryAfter = Math.max(1, Duration.between(now, sanction.blockedUntil).toSeconds());
            return new RateLimitDecision(false, 0, 0, 0, retryAfter, "Temporarily blocked by sanction policy.");
        }

        UserUsage usage = usageByUid.computeIfAbsent(uid, key -> new UserUsage());
        synchronized (usage) {
            evictOld(usage.minuteHits, now.minus(MINUTE_WINDOW));
            evictOld(usage.hourHits, now.minus(HOUR_WINDOW));
            evictOld(usage.dayHits, now.minus(DAY_WINDOW));

            if (usage.minuteHits.size() >= minuteLimit
                    || usage.hourHits.size() >= hourLimit
                    || usage.dayHits.size() >= dayLimit) {
                return applySanction(uid, now);
            }

            usage.minuteHits.addLast(now);
            usage.hourHits.addLast(now);
            usage.dayHits.addLast(now);

            int minuteRemaining = Math.max(0, minuteLimit - usage.minuteHits.size());
            int hourRemaining = Math.max(0, hourLimit - usage.hourHits.size());
            int dayRemaining = Math.max(0, dayLimit - usage.dayHits.size());
            return new RateLimitDecision(true, minuteRemaining, hourRemaining, dayRemaining, 0, "OK");
        }
    }

    private RateLimitDecision applySanction(String uid, Instant now) {
        SanctionState sanction = sanctionsByUid.computeIfAbsent(uid, key -> new SanctionState());
        synchronized (sanction) {
            if (sanction.lastViolationAt == null
                    || Duration.between(sanction.lastViolationAt, now).compareTo(STRIKE_WINDOW) > 0) {
                sanction.strikeCount = 0;
            }
            sanction.strikeCount++;
            sanction.lastViolationAt = now;

            Duration blockDuration = sanction.strikeCount >= 2 ? secondBlockDuration : firstBlockDuration;
            sanction.blockedUntil = now.plus(blockDuration);
            long retryAfter = Math.max(1, blockDuration.toSeconds());
            String reason = sanction.strikeCount >= 2
                    ? "Rate limit exceeded: 2nd strike, blocked for 24 hours."
                    : "Rate limit exceeded: 1st strike, blocked for 15 minutes.";
            return new RateLimitDecision(false, 0, 0, 0, retryAfter, reason);
        }
    }

    private void evictOld(Deque<Instant> queue, Instant threshold) {
        while (!queue.isEmpty() && queue.peekFirst().isBefore(threshold)) {
            queue.pollFirst();
        }
    }

    private static final class UserUsage {
        private final Deque<Instant> minuteHits = new ArrayDeque<>();
        private final Deque<Instant> hourHits = new ArrayDeque<>();
        private final Deque<Instant> dayHits = new ArrayDeque<>();
    }

    private static final class SanctionState {
        private int strikeCount = 0;
        private Instant lastViolationAt;
        private Instant blockedUntil;
    }
}
