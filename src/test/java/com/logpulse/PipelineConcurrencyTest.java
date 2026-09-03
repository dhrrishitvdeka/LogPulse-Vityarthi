package com.logpulse;

import com.logpulse.aggregator.IncidentAggregator;
import com.logpulse.model.AnomalyType;
import com.logpulse.model.Incident;
import com.logpulse.model.SeverityLevel;

import java.time.Instant;
import java.util.List;

public class PipelineConcurrencyTest {

    public static void runAll() {
        testTopKHeap();
    }

    public static void testTopKHeap() {
        IncidentAggregator aggregator = new IncidentAggregator();
        Instant now = Instant.now();

        aggregator.record(new Incident(AnomalyType.RATE_LIMIT_EXCEEDED, SeverityLevel.LOW, "192.168.1.10", now, 1, 60, "test"));

        for (int i = 0; i < 5; i++) {
            aggregator.record(new Incident(AnomalyType.BRUTE_FORCE_AUTH, SeverityLevel.HIGH, "192.168.1.20", now, i + 1, 60, "test"));
        }

        for (int i = 0; i < 3; i++) {
            aggregator.record(new Incident(AnomalyType.SUSPICIOUS_PATH_SCAN, SeverityLevel.HIGH, "192.168.1.30", now, i + 1, 60, "test"));
        }

        List<IncidentAggregator.IpOffenseSummary> top2 = aggregator.getTopOffenders(2);
        if (top2.size() != 2) throw new AssertionError("Expected 2 offenders");
        if (!"192.168.1.20".equals(top2.get(0).ip())) throw new AssertionError("Rank 1 mismatch");
        if (top2.get(0).incidentCount() != 5) throw new AssertionError("Count 1 mismatch");
        if (!"192.168.1.30".equals(top2.get(1).ip())) throw new AssertionError("Rank 2 mismatch");
        if (top2.get(1).incidentCount() != 3) throw new AssertionError("Count 2 mismatch");
    }
}
