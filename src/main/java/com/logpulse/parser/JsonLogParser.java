package com.logpulse.parser;

import com.logpulse.exception.LogParseException;
import com.logpulse.model.HttpMethod;
import com.logpulse.model.LogEntry;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonLogParser implements LogParser {

    private static final Pattern STR_PATTERN = Pattern.compile("\"([a-zA-Z0-9_.-]+)\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern NUM_PATTERN = Pattern.compile("\"([a-zA-Z0-9_.-]+)\"\\s*:\\s*(-?\\d+)");

    @Override
    public LogEntry parse(String rawLine, long lineNumber) throws LogParseException {
        if (rawLine == null || rawLine.isBlank()) {
            throw new LogParseException("Empty line", rawLine, lineNumber);
        }

        String trimmed = rawLine.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw new LogParseException("Invalid JSON payload", rawLine, lineNumber);
        }

        try {
            String clientIp = "127.0.0.1";
            Instant timestamp = Instant.now();
            HttpMethod method = HttpMethod.GET;
            String endpoint = "/";
            int statusCode = 200;
            long bytes = 0;
            long responseTimeMs = 0;
            String userAgent = "-";
            String referer = "-";

            Matcher strMatcher = STR_PATTERN.matcher(trimmed);
            while (strMatcher.find()) {
                String key = strMatcher.group(1).toLowerCase();
                String val = strMatcher.group(2);

                switch (key) {
                    case "client_ip", "clientip", "ip", "remote_addr" -> clientIp = val;
                    case "timestamp", "time", "@timestamp", "date" -> {
                        try {
                            timestamp = Instant.parse(val);
                        } catch (Exception ignored) {}
                    }
                    case "method", "http_method", "verb" -> method = HttpMethod.fromString(val);
                    case "endpoint", "uri", "path", "url" -> endpoint = val;
                    case "user_agent", "useragent" -> userAgent = val;
                    case "referer", "referrer" -> referer = val;
                }
            }

            Matcher numMatcher = NUM_PATTERN.matcher(trimmed);
            while (numMatcher.find()) {
                String key = numMatcher.group(1).toLowerCase();
                long val = Long.parseLong(numMatcher.group(2));

                switch (key) {
                    case "status", "status_code", "code", "http_status" -> statusCode = (int) val;
                    case "bytes", "body_bytes_sent", "size" -> bytes = val;
                    case "response_time_ms", "duration_ms", "latency_ms" -> responseTimeMs = val;
                }
            }

            return LogEntry.builder()
                    .clientIp(clientIp)
                    .timestamp(timestamp)
                    .method(method)
                    .endpoint(endpoint)
                    .statusCode(statusCode)
                    .responseBytes(bytes)
                    .responseTimeMs(responseTimeMs)
                    .userAgent(userAgent)
                    .referer(referer)
                    .rawLine(rawLine)
                    .lineNumber(lineNumber)
                    .build();

        } catch (Exception e) {
            throw new LogParseException("Failed to parse JSON: " + e.getMessage(), rawLine, lineNumber, e);
        }
    }

    @Override
    public boolean canParse(String sampleLine) {
        if (sampleLine == null) return false;
        String t = sampleLine.trim();
        return t.startsWith("{") && t.endsWith("}") && (t.contains("\"status\"") || t.contains("\"method\""));
    }

    @Override
    public String getFormatName() {
        return "JSON";
    }
}
