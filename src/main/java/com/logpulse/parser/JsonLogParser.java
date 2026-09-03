package com.logpulse.parser;

import com.logpulse.exception.LogParseException;
import com.logpulse.model.HttpMethod;
import com.logpulse.model.LogEntry;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-speed, zero-dependency JSON log parser for modern cloud/microservice logs.
 * Extracts standard keys (timestamp, client_ip/ip, method, endpoint/uri/path, status/status_code, etc.)
 */
public class JsonLogParser implements LogParser {

    private static final Pattern STRING_KEY_PATTERN = Pattern.compile("\"([a-zA-Z0-9_.-]+)\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern NUMBER_KEY_PATTERN = Pattern.compile("\"([a-zA-Z0-9_.-]+)\"\\s*:\\s*(-?\\d+)");

    @Override
    public LogEntry parse(String rawLine, long lineNumber) throws LogParseException {
        if (rawLine == null || rawLine.isBlank()) {
            throw new LogParseException("Blank line encountered", rawLine, lineNumber);
        }

        String trimmed = rawLine.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw new LogParseException("Line is not a valid JSON object", rawLine, lineNumber);
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

            // Extract string properties
            Matcher strMatcher = STRING_KEY_PATTERN.matcher(trimmed);
            while (strMatcher.find()) {
                String key = strMatcher.group(1).toLowerCase();
                String val = strMatcher.group(2);

                switch (key) {
                    case "client_ip":
                    case "clientip":
                    case "ip":
                    case "remote_addr":
                        clientIp = val;
                        break;
                    case "timestamp":
                    case "time":
                    case "@timestamp":
                    case "date":
                        try {
                            timestamp = Instant.parse(val);
                        } catch (Exception ignored) {
                        }
                        break;
                    case "method":
                    case "http_method":
                    case "verb":
                        method = HttpMethod.fromString(val);
                        break;
                    case "endpoint":
                    case "uri":
                    case "path":
                    case "url":
                        endpoint = val;
                        break;
                    case "user_agent":
                    case "useragent":
                        userAgent = val;
                        break;
                    case "referer":
                    case "referrer":
                        referer = val;
                        break;
                }
            }

            // Extract numeric properties
            Matcher numMatcher = NUMBER_KEY_PATTERN.matcher(trimmed);
            while (numMatcher.find()) {
                String key = numMatcher.group(1).toLowerCase();
                long val = Long.parseLong(numMatcher.group(2));

                switch (key) {
                    case "status":
                    case "status_code":
                    case "code":
                    case "http_status":
                        statusCode = (int) val;
                        break;
                    case "bytes":
                    case "body_bytes_sent":
                    case "size":
                        bytes = val;
                        break;
                    case "response_time_ms":
                    case "duration_ms":
                    case "latency_ms":
                    case "time_taken":
                        responseTimeMs = val;
                        break;
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
            throw new LogParseException("Failed to parse JSON log line: " + e.getMessage(), rawLine, lineNumber, e);
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
        return "Structured JSON Microservice Format";
    }
}
