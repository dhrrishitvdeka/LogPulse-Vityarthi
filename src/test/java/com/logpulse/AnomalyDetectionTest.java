package com.logpulse;

import com.logpulse.engine.rules.BruteForceRule;
import com.logpulse.engine.rules.ServerErrorBurstRule;
import com.logpulse.engine.rules.SuspiciousScanRule;
import com.logpulse.model.*;

import java.time.Instant;
import java.util.Optional;

public class AnomalyDetectionTest {

    public static void runAll() {
        testBruteForceRule();
        testSuspiciousScanRule();
        testServerErrorBurst();
    }

    public static void testBruteForceRule() {
        BruteForceRule rule = new BruteForceRule(3, 60);
        Instant now = Instant.now();

        LogEntry f1 = LogEntry.builder().clientIp("10.10.10.1").statusCode(401).timestamp(now).build();
        LogEntry f2 = LogEntry.builder().clientIp("10.10.10.1").statusCode(403).timestamp(now.plusSeconds(1)).build();
        LogEntry f3 = LogEntry.builder().clientIp("10.10.10.1").statusCode(401).timestamp(now.plusSeconds(2)).build();

        if (rule.evaluate(f1).isPresent()) throw new AssertionError("Triggered prematurely on 1st fail");
        if (rule.evaluate(f2).isPresent()) throw new AssertionError("Triggered prematurely on 2nd fail");

        Optional<Incident> inc = rule.evaluate(f3);
        if (inc.isEmpty()) throw new AssertionError("Expected incident on 3rd failure");
        if (inc.get().getAnomalyType() != AnomalyType.BRUTE_FORCE_AUTH) throw new AssertionError("Type mismatch");
    }

    public static void testSuspiciousScanRule() {
        SuspiciousScanRule rule = new SuspiciousScanRule();

        LogEntry safe = LogEntry.builder().clientIp("1.2.3.4").endpoint("/index.html").build();
        if (rule.evaluate(safe).isPresent()) throw new AssertionError("Triggered on safe endpoint");

        LogEntry probe = LogEntry.builder().clientIp("1.2.3.4").endpoint("/wp-admin/login.php").build();
        Optional<Incident> inc = rule.evaluate(probe);
        if (inc.isEmpty()) throw new AssertionError("Expected scan detection");
        if (inc.get().getAnomalyType() != AnomalyType.SUSPICIOUS_PATH_SCAN) throw new AssertionError("Type mismatch");
    }

    public static void testServerErrorBurst() {
        ServerErrorBurstRule rule = new ServerErrorBurstRule(2, 60);
        Instant now = Instant.now();

        LogEntry err1 = LogEntry.builder().endpoint("/checkout").statusCode(500).timestamp(now).build();
        LogEntry err2 = LogEntry.builder().endpoint("/checkout").statusCode(503).timestamp(now.plusSeconds(1)).build();

        if (rule.evaluate(err1).isPresent()) throw new AssertionError("Triggered on 1st error");
        Optional<Incident> inc = rule.evaluate(err2);
        if (inc.isEmpty()) throw new AssertionError("Expected 5xx burst incident");
        if (inc.get().getAnomalyType() != AnomalyType.SERVER_ERROR_BURST) throw new AssertionError("Type mismatch");
    }
}
