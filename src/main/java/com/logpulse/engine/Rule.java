package com.logpulse.engine;

import com.logpulse.model.Incident;
import com.logpulse.model.LogEntry;

import java.util.Optional;

/**
 * Functional abstraction for an anomaly detection or rate limiting rule.
 */
public interface Rule {

    /**
     * Evaluates an incoming log entry against the rule's criteria.
     *
     * @param entry The parsed LogEntry.
     * @return An Optional containing an Incident if an anomaly is detected, or empty otherwise.
     */
    Optional<Incident> evaluate(LogEntry entry);

    /**
     * Name of the rule for identification and metrics.
     */
    String getRuleName();
}
