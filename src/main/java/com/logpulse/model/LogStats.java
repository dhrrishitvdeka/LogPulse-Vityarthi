package com.logpulse.model;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe statistics and telemetry container for a pipeline run.
 */
public class LogStats {
    private final Instant startTime;
    private volatile Instant endTime;
    private final AtomicLong totalLinesRead = new AtomicLong(0);
    private final AtomicLong validLinesParsed = new AtomicLong(0);
    private final AtomicLong malformedLines = new AtomicLong(0);
    private final AtomicLong totalBytesProcessed = new AtomicLong(0);
    private final AtomicLong totalIncidentsGenerated = new AtomicLong(0);
    private final ConcurrentHashMap<Integer, AtomicLong> statusCodeCounts = new ConcurrentHashMap<>();

    public LogStats() {
        this.startTime = Instant.now();
    }

    public void incrementLinesRead() {
        totalLinesRead.incrementAndGet();
    }

    public void incrementValidParsed() {
        validLinesParsed.incrementAndGet();
    }

    public void incrementMalformed() {
        malformedLines.incrementAndGet();
    }

    public void addBytes(long bytes) {
        totalBytesProcessed.addAndGet(bytes);
    }

    public void incrementIncidents() {
        totalIncidentsGenerated.incrementAndGet();
    }

    public void recordStatusCode(int statusCode) {
        statusCodeCounts.computeIfAbsent(statusCode, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void finish() {
        this.endTime = Instant.now();
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime != null ? endTime : Instant.now();
    }

    public long getTotalLinesRead() {
        return totalLinesRead.get();
    }

    public long getValidLinesParsed() {
        return validLinesParsed.get();
    }

    public long getMalformedLines() {
        return malformedLines.get();
    }

    public long getTotalBytesProcessed() {
        return totalBytesProcessed.get();
    }

    public long getTotalIncidentsGenerated() {
        return totalIncidentsGenerated.get();
    }

    public ConcurrentHashMap<Integer, AtomicLong> getStatusCodeCounts() {
        return statusCodeCounts;
    }

    public double getElapsedSeconds() {
        Instant finish = endTime != null ? endTime : Instant.now();
        long millis = Duration.between(startTime, finish).toMillis();
        return Math.max(0.001, millis / 1000.0);
    }

    public double getThroughputLinesPerSecond() {
        return totalLinesRead.get() / getElapsedSeconds();
    }

    public double getThroughputMegabytesPerSecond() {
        double megabytes = totalBytesProcessed.get() / (1024.0 * 1024.0);
        return megabytes / getElapsedSeconds();
    }
}
