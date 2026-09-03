package com.logpulse.model;

/**
 * Severity ranking for security incidents and operational anomalies.
 */
public enum SeverityLevel {
    INFO(1, "\u001B[36m"),      // Cyan
    LOW(2, "\u001B[32m"),       // Green
    MEDIUM(3, "\u001B[33m"),    // Yellow
    HIGH(4, "\u001B[35m"),      // Magenta
    CRITICAL(5, "\u001B[31m");  // Red

    private final int rank;
    private final String ansiColor;

    SeverityLevel(int rank, String ansiColor) {
        this.rank = rank;
        this.ansiColor = ansiColor;
    }

    public int getRank() {
        return rank;
    }

    public String getAnsiColor() {
        return ansiColor;
    }
}
