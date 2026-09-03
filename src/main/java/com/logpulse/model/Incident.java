package com.logpulse.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a security or operational incident flagged by the anomaly detection engine.
 */
public final class Incident implements Comparable<Incident> {
    private final String incidentId;
    private final AnomalyType anomalyType;
    private final SeverityLevel severity;
    private final String clientIp;
    private final Instant detectedAt;
    private final int eventCount;
    private final long windowSeconds;
    private final String details;

    public Incident(AnomalyType anomalyType, SeverityLevel severity, String clientIp,
                    Instant detectedAt, int eventCount, long windowSeconds, String details) {
        this.incidentId = UUID.randomUUID().toString().substring(0, 8);
        this.anomalyType = Objects.requireNonNull(anomalyType, "anomalyType cannot be null");
        this.severity = Objects.requireNonNull(severity, "severity cannot be null");
        this.clientIp = Objects.requireNonNull(clientIp, "clientIp cannot be null");
        this.detectedAt = Objects.requireNonNull(detectedAt, "detectedAt cannot be null");
        this.eventCount = eventCount;
        this.windowSeconds = windowSeconds;
        this.details = details != null ? details : "";
    }

    public String getIncidentId() {
        return incidentId;
    }

    public AnomalyType getAnomalyType() {
        return anomalyType;
    }

    public SeverityLevel getSeverity() {
        return severity;
    }

    public String getClientIp() {
        return clientIp;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public int getEventCount() {
        return eventCount;
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public int compareTo(Incident other) {
        // Highest severity first; if equal, highest event count first
        int severityComparison = Integer.compare(other.severity.getRank(), this.severity.getRank());
        if (severityComparison != 0) {
            return severityComparison;
        }
        return Integer.compare(other.eventCount, this.eventCount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Incident incident)) return false;
        return Objects.equals(incidentId, incident.incidentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(incidentId);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | IP: %-15s | Count: %-4d | %s",
                severity, anomalyType, clientIp, eventCount, details);
    }
}
