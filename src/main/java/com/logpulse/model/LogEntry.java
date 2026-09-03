package com.logpulse.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable domain representation of an individual parsed server log entry.
 */
public final class LogEntry {
    private final String clientIp;
    private final Instant timestamp;
    private final HttpMethod method;
    private final String endpoint;
    private final String httpVersion;
    private final int statusCode;
    private final long responseBytes;
    private final long responseTimeMs;
    private final String userAgent;
    private final String referer;
    private final String rawLine;
    private final long lineNumber;

    private LogEntry(Builder builder) {
        this.clientIp = Objects.requireNonNull(builder.clientIp, "clientIp cannot be null");
        this.timestamp = Objects.requireNonNull(builder.timestamp, "timestamp cannot be null");
        this.method = builder.method != null ? builder.method : HttpMethod.UNKNOWN;
        this.endpoint = builder.endpoint != null ? builder.endpoint : "/";
        this.httpVersion = builder.httpVersion != null ? builder.httpVersion : "HTTP/1.1";
        this.statusCode = builder.statusCode;
        this.responseBytes = builder.responseBytes;
        this.responseTimeMs = builder.responseTimeMs;
        this.userAgent = builder.userAgent != null ? builder.userAgent : "-";
        this.referer = builder.referer != null ? builder.referer : "-";
        this.rawLine = builder.rawLine != null ? builder.rawLine : "";
        this.lineNumber = builder.lineNumber;
    }

    public String getClientIp() {
        return clientIp;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getResponseBytes() {
        return responseBytes;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getReferer() {
        return referer;
    }

    public String getRawLine() {
        return rawLine;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }

    public boolean isAuthFailure() {
        return statusCode == 401 || statusCode == 403;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogEntry logEntry)) return false;
        return statusCode == logEntry.statusCode &&
               responseBytes == logEntry.responseBytes &&
               responseTimeMs == logEntry.responseTimeMs &&
               lineNumber == logEntry.lineNumber &&
               Objects.equals(clientIp, logEntry.clientIp) &&
               Objects.equals(timestamp, logEntry.timestamp) &&
               method == logEntry.method &&
               Objects.equals(endpoint, logEntry.endpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientIp, timestamp, method, endpoint, statusCode, lineNumber);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %s %s -> %d (%d bytes, %d ms)",
                timestamp, clientIp, method, endpoint, statusCode, responseBytes, responseTimeMs);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String clientIp = "127.0.0.1";
        private Instant timestamp = Instant.now();
        private HttpMethod method = HttpMethod.GET;
        private String endpoint = "/";
        private String httpVersion = "HTTP/1.1";
        private int statusCode = 200;
        private long responseBytes = 0;
        private long responseTimeMs = 0;
        private String userAgent = "-";
        private String referer = "-";
        private String rawLine = "";
        private long lineNumber = 0;

        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder httpVersion(String httpVersion) {
            this.httpVersion = httpVersion;
            return this;
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder responseBytes(long responseBytes) {
            this.responseBytes = responseBytes;
            return this;
        }

        public Builder responseTimeMs(long responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder referer(String referer) {
            this.referer = referer;
            return this;
        }

        public Builder rawLine(String rawLine) {
            this.rawLine = rawLine;
            return this;
        }

        public Builder lineNumber(long lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public LogEntry build() {
            return new LogEntry(this);
        }
    }
}
