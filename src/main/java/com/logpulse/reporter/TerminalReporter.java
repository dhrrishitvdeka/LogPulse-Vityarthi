package com.logpulse.reporter;

import com.logpulse.aggregator.IncidentAggregator;
import com.logpulse.model.AnomalyType;
import com.logpulse.model.Incident;
import com.logpulse.model.LogStats;
import com.logpulse.model.SeverityLevel;

import java.util.List;
import java.util.Map;

/**
 * Terminal dashboard rendering structured CLI tables with ANSI color codes.
 */
public class TerminalReporter {

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String BLUE = "\u001B[34m";

    public void renderDashboard(LogStats stats, IncidentAggregator aggregator, int topK) {
        printBanner();
        printTelemetry(stats);
        printStatusDistribution(stats);
        printAnomalySummary(aggregator);
        printTopOffenders(aggregator, topK);
        printRecentIncidents(aggregator, 8);
        printFooter();
    }

    private void printBanner() {
        System.out.println(BOLD + BLUE + "================================================================================" + RESET);
        System.out.println(BOLD + CYAN + "               LOGPULSE // HIGH-THROUGHPUT ANOMALY ENGINE                       " + RESET);
        System.out.println(BOLD + BLUE + "================================================================================" + RESET);
    }

    private void printTelemetry(LogStats stats) {
        System.out.println("\n" + BOLD + "[ 1. PIPELINE TELEMETRY & PERFORMANCE ]" + RESET);
        System.out.printf("  %-25s: %.3f seconds%n", "Total Processing Time", stats.getElapsedSeconds());
        System.out.printf("  %-25s: %,d lines (%,.2f MB)%n", "Total Lines Processed",
                stats.getTotalLinesRead(), stats.getTotalBytesProcessed() / (1024.0 * 1024.0));
        System.out.printf("  %-25s: %,d%n", "Valid Lines Parsed", stats.getValidLinesParsed());
        System.out.printf("  %-25s: %,d%n", "Malformed / Skipped", stats.getMalformedLines());
        System.out.printf("  %-25s: " + GREEN + BOLD + "%,.0f lines/sec (%,.2f MB/sec)" + RESET + "%n",
                "Throughput", stats.getThroughputLinesPerSecond(), stats.getThroughputMegabytesPerSecond());
    }

    private void printStatusDistribution(LogStats stats) {
        System.out.println("\n" + BOLD + "[ 2. HTTP STATUS DISTRIBUTION ]" + RESET);
        Map<Integer, ?> counts = stats.getStatusCodeCounts();
        if (counts.isEmpty()) {
            System.out.println("  No HTTP status codes recorded.");
            return;
        }

        long status2xx = 0, status3xx = 0, status4xx = 0, status5xx = 0;
        for (Map.Entry<Integer, ?> entry : counts.entrySet()) {
            int code = entry.getKey();
            long count = ((Number) entry.getValue()).longValue();
            if (code >= 200 && code < 300) status2xx += count;
            else if (code >= 300 && code < 400) status3xx += count;
            else if (code >= 400 && code < 500) status4xx += count;
            else if (code >= 500) status5xx += count;
        }

        System.out.printf("  " + GREEN + "2xx Success: %,d" + RESET + " | " +
                          CYAN + "3xx Redirect: %,d" + RESET + " | " +
                          YELLOW + "4xx Client Error: %,d" + RESET + " | " +
                          RED + "5xx Server Error: %,d" + RESET + "%n",
                status2xx, status3xx, status4xx, status5xx);
    }

    private void printAnomalySummary(IncidentAggregator aggregator) {
        System.out.println("\n" + BOLD + "[ 3. DETECTED ANOMALY BREAKDOWN ]" + RESET);
        System.out.printf("  Total Incidents Flagged: " + (aggregator.getTotalIncidentCount() > 0 ? RED + BOLD : GREEN)
                + "%,d" + RESET + "%n", aggregator.getTotalIncidentCount());

        Map<AnomalyType, Integer> typeCounts = aggregator.getAnomalyTypeCounts();
        if (typeCounts.isEmpty()) {
            System.out.println("  " + GREEN + "✔ Clean log stream. Zero anomalies detected." + RESET);
            return;
        }

        for (Map.Entry<AnomalyType, Integer> entry : typeCounts.entrySet()) {
            System.out.printf("  - %-32s: %,d incident(s)%n", entry.getKey().name(), entry.getValue());
        }
    }

    private void printTopOffenders(IncidentAggregator aggregator, int topK) {
        System.out.println("\n" + BOLD + "[ 4. TOP " + topK + " OFFENDING IP ADDRESSES ]" + RESET);
        List<IncidentAggregator.IpOffenseSummary> offenders = aggregator.getTopOffenders(topK);

        if (offenders.isEmpty()) {
            System.out.println("  No offending IP addresses recorded.");
            return;
        }

        System.out.printf("  +----+-----------------+------------+---------------+----------------------------------+%n");
        System.out.printf("  | #  | IP Address      | Incidents  | Max Severity  | Primary Anomaly Vector           |%n");
        System.out.printf("  +----+-----------------+------------+---------------+----------------------------------+%n");

        int rank = 1;
        for (IncidentAggregator.IpOffenseSummary offender : offenders) {
            String primaryVector = offender.typeBreakdown().entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey().name())
                    .orElse("UNKNOWN");

            SeverityLevel sev = offender.maxSeverity();
            String sevStr = sev.getAnsiColor() + String.format("%-13s", sev.name()) + RESET;

            System.out.printf("  | %-2d | %-15s | %-10d | %s | %-32s |%n",
                    rank++, offender.ip(), offender.incidentCount(), sevStr, primaryVector);
        }
        System.out.printf("  +----+-----------------+------------+---------------+----------------------------------+%n");
    }

    private void printRecentIncidents(IncidentAggregator aggregator, int maxDisplay) {
        List<Incident> incidents = aggregator.getAllIncidents();
        if (incidents.isEmpty()) return;

        System.out.println("\n" + BOLD + "[ 5. RECENT CRITICAL INCIDENTS AUDIT TRAIL ]" + RESET);
        int start = Math.max(0, incidents.size() - maxDisplay);
        for (int i = start; i < incidents.size(); i++) {
            Incident inc = incidents.get(i);
            System.out.printf("  %s[%s]%s %s %-15s -> %s%n",
                    inc.getSeverity().getAnsiColor(), inc.getSeverity().name(), RESET,
                    inc.getAnomalyType().name(), inc.getClientIp(), inc.getDetails());
        }
    }

    private void printFooter() {
        System.out.println("\n" + BLUE + "================================================================================" + RESET);
        System.out.println("LogPulse Execution Completed Successfully. Exit Code: 0");
        System.out.println(BLUE + "================================================================================" + RESET);
    }
}
