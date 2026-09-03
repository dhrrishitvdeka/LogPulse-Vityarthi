package com.logpulse.config;

import com.logpulse.exception.ConfigurationException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class LogPulseConfig {
    private final String logFilePath;
    private final String format;
    private final long slidingWindowSeconds;
    private final int rateLimitThreshold;
    private final int authFailureThreshold;
    private final int serverErrorThreshold;
    private final int topKOffenders;
    private final int workerThreads;
    private final String exportFormat;
    private final String exportPath;
    private final int queueCapacity;

    private LogPulseConfig(Builder builder) {
        this.logFilePath = builder.logFilePath;
        this.format = builder.format;
        this.slidingWindowSeconds = builder.slidingWindowSeconds;
        this.rateLimitThreshold = builder.rateLimitThreshold;
        this.authFailureThreshold = builder.authFailureThreshold;
        this.serverErrorThreshold = builder.serverErrorThreshold;
        this.topKOffenders = builder.topKOffenders;
        this.workerThreads = builder.workerThreads;
        this.exportFormat = builder.exportFormat;
        this.exportPath = builder.exportPath;
        this.queueCapacity = builder.queueCapacity;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public String getFormat() {
        return format;
    }

    public long getSlidingWindowSeconds() {
        return slidingWindowSeconds;
    }

    public int getRateLimitThreshold() {
        return rateLimitThreshold;
    }

    public int getAuthFailureThreshold() {
        return authFailureThreshold;
    }

    public int getServerErrorThreshold() {
        return serverErrorThreshold;
    }

    public int getTopKOffenders() {
        return topKOffenders;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public String getExportPath() {
        return exportPath;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String logFilePath;
        private String format = "auto";
        private long slidingWindowSeconds = 60;
        private int rateLimitThreshold = 50;
        private int authFailureThreshold = 5;
        private int serverErrorThreshold = 10;
        private int topKOffenders = 5;
        private int workerThreads = Math.max(2, Runtime.getRuntime().availableProcessors());
        private String exportFormat = "none";
        private String exportPath = "logpulse_report.json";
        private int queueCapacity = 10_000;

        public Builder logFilePath(String path) {
            this.logFilePath = path;
            return this;
        }

        public Builder format(String format) {
            this.format = format != null ? format.toLowerCase() : "auto";
            return this;
        }

        public Builder slidingWindowSeconds(long seconds) {
            this.slidingWindowSeconds = seconds;
            return this;
        }

        public Builder rateLimitThreshold(int threshold) {
            this.rateLimitThreshold = threshold;
            return this;
        }

        public Builder authFailureThreshold(int threshold) {
            this.authFailureThreshold = threshold;
            return this;
        }

        public Builder serverErrorThreshold(int threshold) {
            this.serverErrorThreshold = threshold;
            return this;
        }

        public Builder topKOffenders(int topK) {
            this.topKOffenders = topK;
            return this;
        }

        public Builder workerThreads(int threads) {
            this.workerThreads = threads;
            return this;
        }

        public Builder exportFormat(String exportFormat) {
            this.exportFormat = exportFormat != null ? exportFormat.toLowerCase() : "none";
            return this;
        }

        public Builder exportPath(String path) {
            this.exportPath = path;
            return this;
        }

        public Builder queueCapacity(int capacity) {
            this.queueCapacity = capacity;
            return this;
        }

        public LogPulseConfig build() {
            if (logFilePath == null || logFilePath.isBlank()) {
                throw new ConfigurationException("Log file path must be specified via --file");
            }
            Path path = Paths.get(logFilePath);
            if (!Files.exists(path)) {
                throw new ConfigurationException("File not found: " + logFilePath);
            }
            if (slidingWindowSeconds <= 0) {
                throw new ConfigurationException("Window duration must be positive");
            }
            if (rateLimitThreshold <= 0 || authFailureThreshold <= 0 || serverErrorThreshold <= 0) {
                throw new ConfigurationException("Thresholds must be positive numbers");
            }
            if (workerThreads <= 0) {
                throw new ConfigurationException("Worker thread count must be positive");
            }
            return new LogPulseConfig(this);
        }
    }
}
