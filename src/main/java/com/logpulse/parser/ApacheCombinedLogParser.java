package com.logpulse.parser;

import com.logpulse.exception.LogParseException;
import com.logpulse.model.HttpMethod;
import com.logpulse.model.LogEntry;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance regex parser for Nginx and Apache Combined Log Formats:
 * %h %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\" [Optional: %D or %T]
 */
public class ApacheCombinedLogParser implements LogParser {

    private static final String APACHE_PATTERN_REGEX =
            "^(\\S+) \\S+ \\S+ \\[([^\\]]+)\\] \"(\\S+) (\\S+) ([^\"]+)\" (\\d{3}) (\\d+|-) \"([^\"]*)\" \"([^\"]*)\"(?: (\\d+))?";

    private final Pattern pattern = Pattern.compile(APACHE_PATTERN_REGEX);

    // Common Apache date format: 10/Oct/2026:13:55:36 +0000
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);

    @Override
    public LogEntry parse(String rawLine, long lineNumber) throws LogParseException {
        if (rawLine == null || rawLine.isBlank()) {
            throw new LogParseException("Blank or null line encountered", rawLine, lineNumber);
        }

        Matcher matcher = pattern.matcher(rawLine);
        if (!matcher.find()) {
            throw new LogParseException("Line does not match Apache Combined format", rawLine, lineNumber);
        }

        try {
            String ip = matcher.group(1);
            String dateStr = matcher.group(2);
            String methodStr = matcher.group(3);
            String endpoint = matcher.group(4);
            String httpVersion = matcher.group(5);
            int statusCode = Integer.parseInt(matcher.group(6));
            String bytesStr = matcher.group(7);
            long bytes = bytesStr.equals("-") ? 0L : Long.parseLong(bytesStr);
            String referer = matcher.group(8);
            String userAgent = matcher.group(9);
            String responseTimeStr = matcher.group(10);
            long responseTimeMs = responseTimeStr != null ? Long.parseLong(responseTimeStr) : 0L;

            Instant timestamp = parseDate(dateStr);

            return LogEntry.builder()
                    .clientIp(ip)
                    .timestamp(timestamp)
                    .method(HttpMethod.fromString(methodStr))
                    .endpoint(endpoint)
                    .httpVersion(httpVersion)
                    .statusCode(statusCode)
                    .responseBytes(bytes)
                    .responseTimeMs(responseTimeMs)
                    .referer(referer)
                    .userAgent(userAgent)
                    .rawLine(rawLine)
                    .lineNumber(lineNumber)
                    .build();

        } catch (Exception e) {
            throw new LogParseException("Failed to extract fields from Apache log entry: " + e.getMessage(),
                    rawLine, lineNumber, e);
        }
    }

    private Instant parseDate(String dateStr) {
        try {
            return ZonedDateTime.parse(dateStr, DATE_FORMATTER).toInstant();
        } catch (DateTimeParseException e) {
            // Fallback for ISO-8601 timestamps
            try {
                return Instant.parse(dateStr);
            } catch (Exception ex) {
                return Instant.now();
            }
        }
    }

    @Override
    public boolean canParse(String sampleLine) {
        if (sampleLine == null) return false;
        return pattern.matcher(sampleLine).find();
    }

    @Override
    public String getFormatName() {
        return "Apache/Nginx Combined Log Format";
    }
}
