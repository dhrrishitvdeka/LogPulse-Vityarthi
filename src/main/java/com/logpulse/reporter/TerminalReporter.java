package com.logpulse.reporter;

import com.logpulse.aggregator.IncidentAggregator;
import com.logpulse.model.AnomalyType;
import com.logpulse.model.Incident;
import com.logpulse.model.LogStats;
import com.logpulse.model.SeverityLevel;

import java.util.List;
import java.util.Map;

public class TerminalReporter {

    public void renderDashboard(LogStats stats, IncidentAggregator aggregator, int topK) {
        System.out.println("----------------------------------------------------------------");
        System.out.println("LogPulse Execution Summary");
        System.out.println("----------------------------------------------------------------");

        System.out.println("\n[Performance]");
        System.out.printf("  Execution Time      : %.3f s%n", stats.getElapsedSeconds());
        System.out.printf("  Lines Processed     : %,d%n", stats.getTotalLinesRead());
        System.out.printf("  Valid Lines Parsed  : %,d%n", stats.getValidLinesParsed());
        System.out.printf("  Malformed / Skipped : %,d%n", stats.getMalformedLines());
        System.out.printf("  Throughput          : %,.0f lines/sec%n", stats.getThroughputLinesPerSecond());

        System.out.println("\n[HTTP Status Breakdown]");
        Map<Integer, ?> counts = stats.getStatusCodeCounts();
        if (counts.isEmpty()) {
            System.out.println("  No HTTP status data.");
        } else {
            long s2xx = 0, s3xx = 0, s4xx = 0, s5xx = 0;
            for (Map.Entry<Integer, ?> entry : counts.entrySet()) {
                int code = entry.getKey();
                long count = ((Number) entry.getValue()).longValue();
                if (code >= 200 && code < 300) s2xx += count;
                else if (code >= 300 && code < 400) s3xx += count;
                else if (code >= 400 && code < 500) s4xx += count;
                else if (code >= 500) s5xx += count;
            }
            System.out.printf("  2xx: %,d | 3xx: %,d | 4xx: %,d | 5xx: %,d%n", s2xx, s3xx, s4xx, s5xx);
        }

        System.out.println("\n[Detected Anomalies]");
        System.out.printf("  Total Incidents: %,d%n", aggregator.getTotalIncidentCount());
        Map<AnomalyType, Integer> typeCounts = aggregator.getAnomalyTypeCounts();
        if (typeCounts.isEmpty()) {
            System.out.println("  No anomalies detected.");
        } else {
            for (Map.Entry<AnomalyType, Integer> entry : typeCounts.entrySet()) {
                System.out.printf("  - %-26s: %,d%n", entry.getKey().name(), entry.getValue());
            }
        }

        System.out.println("\n[Top " + topK + " Offending IPs]");
        List<IncidentAggregator.IpOffenseSummary> offenders = aggregator.getTopOffenders(topK);
        if (offenders.isEmpty()) {
            System.out.println("  No offending IP addresses recorded.");
        } else {
            System.out.printf("  %-4s %-16s %-10s %-10s%n", "No.", "IP Address", "Incidents", "Severity");
            int rank = 1;
            for (IncidentAggregator.IpOffenseSummary offender : offenders) {
                System.out.printf("  %-4d %-16s %-10d %-10s%n",
                        rank++, offender.ip(), offender.incidentCount(), offender.maxSeverity().name());
            }
        }

        List<Incident> incidents = aggregator.getAllIncidents();
        if (!incidents.isEmpty()) {
            System.out.println("\n[Recent Incidents]");
            int start = Math.max(0, incidents.size() - 5);
            for (int i = start; i < incidents.size(); i++) {
                Incident inc = incidents.get(i);
                System.out.printf("  [%s] %s %s - %s%n",
                        inc.getSeverity().name(), inc.getAnomalyType().name(), inc.getClientIp(), inc.getDetails());
            }
        }

        System.out.println("----------------------------------------------------------------");
    }
}
