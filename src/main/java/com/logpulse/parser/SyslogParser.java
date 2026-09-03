package com.logpulse.parser;

import com.logpulse.exception.LogParseException;
import com.logpulse.model.HttpMethod;
import com.logpulse.model.LogEntry;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyslogParser implements LogParser {

    private static final Pattern SYSLOG_PATTERN = Pattern.compile(
            "^(?:<\\d+>)?(?:\\d+ )?(\\S+) (\\S+) (?:[^\\[:]+)(?:\\[\\d+\\])?:? (\\S+) \\S+ \\S+ \\[[^\\]]+\\] \"(\\S+) (\\S+) ([^\"]+)\" (\\d{3}) (\\d+|-)");

    @Override
    public LogEntry parse(String rawLine, long lineNumber) throws LogParseException {
        if (rawLine == null || rawLine.isBlank()) {
            throw new LogParseException("Empty line", rawLine, lineNumber);
        }

        Matcher matcher = SYSLOG_PATTERN.matcher(rawLine.trim());
        if (!matcher.find()) {
            throw new LogParseException("Does not match Syslog web format", rawLine, lineNumber);
        }

        try {
            String timeStr = matcher.group(1);
            String ip = matcher.group(3);
            String methodStr = matcher.group(4);
            String endpoint = matcher.group(5);
            String httpVersion = matcher.group(6);
            int statusCode = Integer.parseInt(matcher.group(7));
            String bytesStr = matcher.group(8);
            long bytes = bytesStr.equals("-") ? 0L : Long.parseLong(bytesStr);

            Instant timestamp;
            try {
                timestamp = Instant.parse(timeStr);
            } catch (Exception e) {
                timestamp = Instant.now();
            }

            return LogEntry.builder()
                    .clientIp(ip)
                    .timestamp(timestamp)
                    .method(HttpMethod.fromString(methodStr))
                    .endpoint(endpoint)
                    .httpVersion(httpVersion)
                    .statusCode(statusCode)
                    .responseBytes(bytes)
                    .rawLine(rawLine)
                    .lineNumber(lineNumber)
                    .build();

        } catch (Exception e) {
            throw new LogParseException("Failed to parse Syslog entry: " + e.getMessage(), rawLine, lineNumber, e);
        }
    }

    @Override
    public boolean canParse(String sampleLine) {
        if (sampleLine == null) return false;
        String t = sampleLine.trim();
        return (t.startsWith("<") || t.contains("nginx[") || t.contains("apache2[")) && SYSLOG_PATTERN.matcher(t).find();
    }

    @Override
    public String getFormatName() {
        return "Syslog";
    }
}
