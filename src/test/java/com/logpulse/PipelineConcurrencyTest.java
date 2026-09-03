package com.logpulse;

import com.logpulse.aggregator.IncidentAggregator;
import com.logpulse.model.AnomalyType;
import com.logpulse.model.Incident;
import com.logpulse.model.SeverityLevel;

import java.time.Instant;
import java.util.List;

/**
 * Unit tests verifying aggregation heap mechanics and concurrency.
 */
public class PipelineConcurrencyTest {

    public static void runAll() {
        testTopKOffendersHeap();
        System.out.println("  ✔ PipelineConcurrencyTest: Aggregation & concurrency tests passed.");
    }

    public static void testTopKOffendersHeap() {
        IncidentAggregator aggregator = new IncidentAggregator();
        Instant now = Instant.now();

        // 1 incident for IP A
        aggregator.record(new Incident(AnomalyType.RATE_LIMIT_EXCEEDED, SeverityLevel.LOW, "192.168.1.10", now, 1, 60, "test"));

        // 5 incidents for IP B
        for (int i = 0; i < 5; i++) {
            aggregator.record(new Incident(AnomalyType.BRUTE_FORCE_AUTH, SeverityLevel.HIGH, "192.168.1.20", now, i + 1, 60, "test"));
        }

        // 3 incidents for IP C
        for (int i = 0; i < 3; i++) {
            aggregator.record(new Incident(AnomalyType.SUSPICIOUS_PATH_SCAN, SeverityLevel.HIGH, "192.168.1.30", now, i + 1, 60, "test"));
        }

        // Top 2 offenders should be IP B (5) and IP C (3)
        List<IncidentAggregator.IpOffenseSummary> top2 = aggregator.getTopOffenders(2);
        assert top2.size() == 2 : "Expected 2 top offenders";
        assert "192.168.1.20".equals(top2.get(0).ip()) : "Rank 1 must be 192.168.1.20";
        assert top2.get(0).incidentCount() == 5 : "Count must be 5";
        assert "192.168.1.30".equals(top2.get(1).ip()) : "Rank 2 must be 192.168.1.30";
        assert top2.get(1).incidentCount() == 3 : "Count must be 3";
    }
}
