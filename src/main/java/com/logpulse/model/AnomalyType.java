package com.logpulse.model;

/**
 * Categorization of security and operational anomalies detected by the engine.
 */
public enum AnomalyType {
    BRUTE_FORCE_AUTH("Repeated 401/403 Authentication Failures"),
    RATE_LIMIT_EXCEEDED("High Request Frequency Exceeding Burst Limit"),
    SUSPICIOUS_PATH_SCAN("Probing Sensitive Endpoints / Vulnerability Scan"),
    SERVER_ERROR_BURST("Spike in 5xx Internal Server Errors"),
    HIGH_LATENCY_SPIKE("Extreme Response Latency Degradation");

    private final String description;

    AnomalyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
