package com.logpulse.engine.rules;

import com.logpulse.engine.Rule;
import com.logpulse.engine.SlidingWindowRateLimiter;
import com.logpulse.model.AnomalyType;
import com.logpulse.model.Incident;
import com.logpulse.model.LogEntry;
import com.logpulse.model.SeverityLevel;

import java.util.Optional;

/**
 * Flags high-volume request floods and potential DoS or web scraping bursts from a single IP.
 */
public class RateLimitRule implements Rule {

    private final SlidingWindowRateLimiter rateLimiter;
    private final int requestLimitThreshold;
    private final long windowSeconds;

    public RateLimitRule(int requestLimitThreshold, long windowSeconds) {
        this.requestLimitThreshold = requestLimitThreshold;
        this.windowSeconds = windowSeconds;
        this.rateLimiter = new SlidingWindowRateLimiter(windowSeconds);
    }

    @Override
    public Optional<Incident> evaluate(LogEntry entry) {
        String key = "RATE_LIMIT:" + entry.getClientIp();
        int count = rateLimiter.recordAndCount(key, entry.getTimestamp());

        if (count >= requestLimitThreshold && (count == requestLimitThreshold || count % (requestLimitThreshold / 2) == 0)) {
            SeverityLevel severity = (count >= requestLimitThreshold * 2) ? SeverityLevel.CRITICAL : SeverityLevel.MEDIUM;

            String details = String.format("Request volume (%d reqs) exceeded rate-limit threshold (%d) within %ds window",
                    count, requestLimitThreshold, windowSeconds);

            return Optional.of(new Incident(
                    AnomalyType.RATE_LIMIT_EXCEEDED,
                    severity,
                    entry.getClientIp(),
                    entry.getTimestamp(),
                    count,
                    windowSeconds,
                    details
            ));
        }

        return Optional.empty();
    }

    @Override
    public String getRuleName() {
        return "RateLimitSpikeRule";
    }
}
