package com.logpulse.engine;

import com.logpulse.config.LogPulseConfig;
import com.logpulse.engine.rules.BruteForceRule;
import com.logpulse.engine.rules.RateLimitRule;
import com.logpulse.engine.rules.ServerErrorBurstRule;
import com.logpulse.engine.rules.SuspiciousScanRule;
import com.logpulse.model.Incident;
import com.logpulse.model.LogEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Orchestrator for evaluating log entries against all active anomaly detection rules.
 */
public class AnomalyDetectionEngine {

    private final List<Rule> activeRules = new CopyOnWriteArrayList<>();

    public AnomalyDetectionEngine(LogPulseConfig config) {
        // Register default configured rules
        registerRule(new BruteForceRule(config.getAuthFailureThreshold(), config.getSlidingWindowSeconds()));
        registerRule(new RateLimitRule(config.getRateLimitThreshold(), config.getSlidingWindowSeconds()));
        registerRule(new ServerErrorBurstRule(config.getServerErrorThreshold(), config.getSlidingWindowSeconds()));
        registerRule(new SuspiciousScanRule());
    }

    public void registerRule(Rule rule) {
        if (rule != null) {
            activeRules.add(rule);
        }
    }

    /**
     * Evaluates a single LogEntry across all registered rules.
     *
     * @param entry The parsed LogEntry.
     * @return List of triggered incidents (empty if no anomalies detected).
     */
    public List<Incident> evaluate(LogEntry entry) {
        if (entry == null) {
            return Collections.emptyList();
        }

        List<Incident> detected = new ArrayList<>(2);
        for (Rule rule : activeRules) {
            rule.evaluate(entry).ifPresent(detected::add);
        }
        return detected;
    }

    public List<Rule> getActiveRules() {
        return Collections.unmodifiableList(activeRules);
    }
}
