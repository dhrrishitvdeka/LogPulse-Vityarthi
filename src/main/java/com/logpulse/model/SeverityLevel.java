package com.logpulse.model;

public enum SeverityLevel {
    INFO(1, "\u001B[36m"),
    LOW(2, "\u001B[32m"),
    MEDIUM(3, "\u001B[33m"),
    HIGH(4, "\u001B[35m"),
    CRITICAL(5, "\u001B[31m");

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
