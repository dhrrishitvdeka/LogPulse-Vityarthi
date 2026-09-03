package com.logpulse.model;

public enum AnomalyType {
    BRUTE_FORCE_AUTH("Repeated 401/403 Failures"),
    RATE_LIMIT_EXCEEDED("High Request Rate Burst"),
    SUSPICIOUS_PATH_SCAN("Suspicious Path / Exploit Probe"),
    SERVER_ERROR_BURST("5xx Internal Server Error Spike");

    private final String description;

    AnomalyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
