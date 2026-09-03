package com.logpulse.engine.rules;

import com.logpulse.engine.Rule;
import com.logpulse.engine.SlidingWindowRateLimiter;
import com.logpulse.model.AnomalyType;
import com.logpulse.model.Incident;
import com.logpulse.model.LogEntry;
import com.logpulse.model.SeverityLevel;

import java.util.Optional;

/**
 * Detects brute force authentication attempts by tracking repeated 401/403 HTTP failures per IP.
 */
public class BruteForceRule implements Rule {

    private final SlidingWindowRateLimiter rateLimiter;
    private final int failureThreshold;
    private final long windowSeconds;

    public BruteForceRule(int failureThreshold, long windowSeconds) {
        this.failureThreshold = failureThreshold;
        this.windowSeconds = windowSeconds;
        this.rateLimiter = new SlidingWindowRateLimiter(windowSeconds);
    }

    @Override
    public Optional<Incident> evaluate(LogEntry entry) {
        if (!entry.isAuthFailure()) {
            return Optional.empty();
        }

        String key = "AUTH_FAIL:" + entry.getClientIp();
        int failures = rateLimiter.recordAndCount(key, entry.getTimestamp());

        // Emit incident when threshold is reached or when severe bursts continue
        if (failures >= failureThreshold && (failures == failureThreshold || failures % failureThreshold == 0)) {
            SeverityLevel severity = (failures >= failureThreshold * 2) ? SeverityLevel.CRITICAL : SeverityLevel.HIGH;

            String details = String.format("Observed %d consecutive 401/403 failures on '%s' within %ds window",
                    failures, entry.getEndpoint(), windowSeconds);

            return Optional.of(new Incident(
                    AnomalyType.BRUTE_FORCE_AUTH,
                    severity,
                    entry.getClientIp(),
                    entry.getTimestamp(),
                    failures,
                    windowSeconds,
                    details
            ));
        }

        return Optional.empty();
    }

    @Override
    public String getRuleName() {
        return "BruteForceAuthenticationRule";
    }
}
