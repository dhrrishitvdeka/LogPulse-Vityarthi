package com.logpulse.engine.rules;

import com.logpulse.engine.Rule;
import com.logpulse.engine.SlidingWindowRateLimiter;
import com.logpulse.model.AnomalyType;
import com.logpulse.model.Incident;
import com.logpulse.model.LogEntry;
import com.logpulse.model.SeverityLevel;

import java.util.Optional;

/**
 * Flags sudden spikes in HTTP 5xx responses signaling upstream backend failures or application crashes.
 */
public class ServerErrorBurstRule implements Rule {

    private final SlidingWindowRateLimiter rateLimiter;
    private final int errorThreshold;
    private final long windowSeconds;

    public ServerErrorBurstRule(int errorThreshold, long windowSeconds) {
        this.errorThreshold = errorThreshold;
        this.windowSeconds = windowSeconds;
        this.rateLimiter = new SlidingWindowRateLimiter(windowSeconds);
    }

    @Override
    public Optional<Incident> evaluate(LogEntry entry) {
        if (!entry.isServerError()) {
            return Optional.empty();
        }

        String key = "SERVER_5XX:" + entry.getEndpoint();
        int count = rateLimiter.recordAndCount(key, entry.getTimestamp());

        if (count >= errorThreshold && (count == errorThreshold || count % errorThreshold == 0)) {
            String details = String.format("Observed %d HTTP 5xx errors on endpoint '%s' within %ds window",
                    count, entry.getEndpoint(), windowSeconds);

            return Optional.of(new Incident(
                    AnomalyType.SERVER_ERROR_BURST,
                    SeverityLevel.HIGH,
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
        return "ServerErrorBurstRule";
    }
}
