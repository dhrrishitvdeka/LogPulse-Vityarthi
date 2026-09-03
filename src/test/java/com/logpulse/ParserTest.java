package com.logpulse;

import com.logpulse.exception.LogParseException;
import com.logpulse.model.HttpMethod;
import com.logpulse.model.LogEntry;
import com.logpulse.parser.ApacheCombinedLogParser;
import com.logpulse.parser.JsonLogParser;
import com.logpulse.parser.SyslogParser;

public class ParserTest {

    public static void runAll() {
        testApacheParser();
        testApacheInvalid();
        testJsonParser();
        testSyslogParser();
    }

    public static void testApacheParser() {
        ApacheCombinedLogParser parser = new ApacheCombinedLogParser();
        String line = "192.168.1.100 - - [03/Sep/2026:14:20:10 +0000] \"GET /api/v1/users HTTP/1.1\" 200 4523 \"https://example.com\" \"Mozilla/5.0\" 45";

        LogEntry entry = parser.parse(line, 1);
        if (!"192.168.1.100".equals(entry.getClientIp())) throw new AssertionError("IP mismatch");
        if (entry.getMethod() != HttpMethod.GET) throw new AssertionError("Method mismatch");
        if (!"/api/v1/users".equals(entry.getEndpoint())) throw new AssertionError("Endpoint mismatch");
        if (entry.getStatusCode() != 200) throw new AssertionError("Status code mismatch");
        if (entry.getResponseBytes() != 4523) throw new AssertionError("Bytes mismatch");
        if (entry.getResponseTimeMs() != 45) throw new AssertionError("Latency mismatch");
    }

    public static void testApacheInvalid() {
        ApacheCombinedLogParser parser = new ApacheCombinedLogParser();
        String invalid = "malformed log line without fields";
        try {
            parser.parse(invalid, 2);
            throw new AssertionError("Expected LogParseException");
        } catch (LogParseException e) {
            if (e.getLineNumber() != 2) throw new AssertionError("Line number mismatch");
        }
    }

    public static void testJsonParser() {
        JsonLogParser parser = new JsonLogParser();
        String line = "{\"timestamp\":\"2026-09-03T10:15:30Z\",\"client_ip\":\"10.0.0.55\",\"method\":\"POST\",\"endpoint\":\"/api/v1/login\",\"status\":401,\"bytes\":128,\"duration_ms\":32}";

        LogEntry entry = parser.parse(line, 3);
        if (!"10.0.0.55".equals(entry.getClientIp())) throw new AssertionError("IP mismatch");
        if (entry.getMethod() != HttpMethod.POST) throw new AssertionError("Method mismatch");
        if (entry.getStatusCode() != 401) throw new AssertionError("Status mismatch");
        if (!entry.isAuthFailure()) throw new AssertionError("Expected auth failure");
    }

    public static void testSyslogParser() {
        SyslogParser parser = new SyslogParser();
        String line = "<134>1 2026-09-03T12:00:00Z gateway nginx: 172.16.0.4 - - [03/Sep/2026:12:00:00 +0000] \"GET /status HTTP/1.1\" 200 150";

        LogEntry entry = parser.parse(line, 4);
        if (!"172.16.0.4".equals(entry.getClientIp())) throw new AssertionError("Syslog IP mismatch");
        if (entry.getStatusCode() != 200) throw new AssertionError("Syslog status mismatch");
    }
}
