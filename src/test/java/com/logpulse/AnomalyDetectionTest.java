package com.logpulse;

import com.logpulse.engine.rules.BruteForceRule;
import com.logpulse.engine.rules.ServerErrorBurstRule;
import com.logpulse.engine.rules.SuspiciousScanRule;
import com.logpulse.model.*;

import java.time.Instant;
import java.util.Optional;

/**
 * Unit tests verifying anomaly rules and incident triggers.
 */
public class AnomalyDetectionTest {

    public static void runAll() {
        testBruteForceRuleTrigger();
        testSuspiciousScanRule();
        testServerErrorBurstRule();
        System.out.println("  ✔ AnomalyDetectionTest: All detection rule test suites passed.");
    }

    public static void testBruteForceRuleTrigger() {
        BruteForceRule rule = new BruteForceRule(3, 60);
        Instant now = Instant.now();

        LogEntry failure1 = LogEntry.builder().clientIp("10.10.10.1").statusCode(401).timestamp(now).build();
        LogEntry failure2 = LogEntry.builder().clientIp("10.10.10.1").statusCode(403).timestamp(now.plusSeconds(1)).build();
        LogEntry failure3 = LogEntry.builder().clientIp("10.10.10.1").statusCode(401).timestamp(now.plusSeconds(2)).build();

        assert rule.evaluate(failure1).isEmpty() : "Should not trigger on 1st failure";
        assert rule.evaluate(failure2).isEmpty() : "Should not trigger on 2nd failure";

        Optional<Incident> incident = rule.evaluate(failure3);
        assert incident.isPresent() : "Must trigger incident on 3rd failure";
        assert incident.get().getAnomalyType() == AnomalyType.BRUTE_FORCE_AUTH;
        assert incident.get().getSeverity() == SeverityLevel.HIGH;
    }

    public static void testSuspiciousScanRule() {
        SuspiciousScanRule rule = new SuspiciousScanRule();

        LogEntry safeEntry = LogEntry.builder().clientIp("1.2.3.4").endpoint("/index.html").build();
        assert rule.evaluate(safeEntry).isEmpty() : "Safe endpoint must not trigger";

        LogEntry probeEntry = LogEntry.builder().clientIp("1.2.3.4").endpoint("/wp-admin/install.php").build();
        Optional<Incident> incident = rule.evaluate(probeEntry);
        assert incident.isPresent() : "Sensitive endpoint must trigger scan anomaly";
        assert incident.get().getAnomalyType() == AnomalyType.SUSPICIOUS_PATH_SCAN;
    }

    public static void testServerErrorBurstRule() {
        ServerErrorBurstRule rule = new ServerErrorBurstRule(2, 60);
        Instant now = Instant.now();

        LogEntry err1 = LogEntry.builder().endpoint("/checkout").statusCode(500).timestamp(now).build();
        LogEntry err2 = LogEntry.builder().endpoint("/checkout").statusCode(503).timestamp(now.plusSeconds(1)).build();

        assert rule.evaluate(err1).isEmpty();
        Optional<Incident> incident = rule.evaluate(err2);
        assert incident.isPresent();
        assert incident.get().getAnomalyType() == AnomalyType.SERVER_ERROR_BURST;
    }
}
