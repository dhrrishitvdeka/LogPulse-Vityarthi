package com.logpulse;

import com.logpulse.exception.LogParseException;
import com.logpulse.model.HttpMethod;
import com.logpulse.model.LogEntry;
import com.logpulse.parser.ApacheCombinedLogParser;
import com.logpulse.parser.JsonLogParser;
import com.logpulse.parser.SyslogParser;

/**
 * Verification test suite for log parser strategies.
 */
public class ParserTest {

    public static void runAll() {
        testApacheParserSuccess();
        testApacheParserInvalid();
        testJsonParserSuccess();
        testSyslogParserSuccess();
        System.out.println("  ✔ ParserTest: All parser test suites passed.");
    }

    public static void testApacheParserSuccess() {
        ApacheCombinedLogParser parser = new ApacheCombinedLogParser();
        String line = "192.168.1.100 - - [03/Sep/2026:14:20:10 +0000] \"GET /api/v1/users HTTP/1.1\" 200 4523 \"https://example.com\" \"Mozilla/5.0\" 45";

        LogEntry entry = parser.parse(line, 1);
        assert "192.168.1.100".equals(entry.getClientIp()) : "IP mismatch";
        assert entry.getMethod() == HttpMethod.GET : "Method mismatch";
        assert "/api/v1/users".equals(entry.getEndpoint()) : "Endpoint mismatch";
        assert entry.getStatusCode() == 200 : "Status code mismatch";
        assert entry.getResponseBytes() == 4523 : "Bytes mismatch";
        assert entry.getResponseTimeMs() == 45 : "Latency mismatch";
    }

    public static void testApacheParserInvalid() {
        ApacheCombinedLogParser parser = new ApacheCombinedLogParser();
        String invalidLine = "This is a corrupted log line that does not conform";

        try {
            parser.parse(invalidLine, 2);
            assert false : "Expected LogParseException on invalid line";
        } catch (LogParseException e) {
            assert e.getLineNumber() == 2;
        }
    }

    public static void testJsonParserSuccess() {
        JsonLogParser parser = new JsonLogParser();
        String line = "{\"timestamp\":\"2026-09-03T10:15:30Z\",\"client_ip\":\"10.0.0.55\",\"method\":\"POST\",\"endpoint\":\"/api/v1/login\",\"status\":401,\"bytes\":128,\"duration_ms\":32}";

        LogEntry entry = parser.parse(line, 3);
        assert "10.0.0.55".equals(entry.getClientIp()) : "JSON IP mismatch";
        assert entry.getMethod() == HttpMethod.POST : "JSON Method mismatch";
        assert entry.getStatusCode() == 401 : "JSON Status mismatch";
        assert entry.isAuthFailure() : "Expected isAuthFailure to be true";
    }

    public static void testSyslogParserSuccess() {
        SyslogParser parser = new SyslogParser();
        String line = "<134>1 2026-09-03T12:00:00Z gateway nginx: 172.16.0.4 - - [03/Sep/2026:12:00:00 +0000] \"GET /status HTTP/1.1\" 200 150";

        LogEntry entry = parser.parse(line, 4);
        assert "172.16.0.4".equals(entry.getClientIp()) : "Syslog IP mismatch";
        assert entry.getStatusCode() == 200 : "Syslog status mismatch";
    }
}
