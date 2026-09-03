package com.logpulse.reporter;

import com.logpulse.aggregator.IncidentAggregator;
import com.logpulse.model.AnomalyType;
import com.logpulse.model.Incident;
import com.logpulse.model.LogStats;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class JsonReportExporter implements ReportExporter {

    @Override
    public void export(LogStats stats, IncidentAggregator aggregator, Path outputPath) throws IOException {
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"timestamp\": \"").append(stats.getEndTime()).append("\",\n");
        sb.append("  \"telemetry\": {\n");
        sb.append("    \"elapsedSeconds\": ").append(String.format("%.3f", stats.getElapsedSeconds())).append(",\n");
        sb.append("    \"totalLinesRead\": ").append(stats.getTotalLinesRead()).append(",\n");
        sb.append("    \"validLinesParsed\": ").append(stats.getValidLinesParsed()).append(",\n");
        sb.append("    \"malformedLines\": ").append(stats.getMalformedLines()).append(",\n");
        sb.append("    \"throughputLinesPerSec\": ").append(String.format("%.2f", stats.getThroughputLinesPerSecond())).append(",\n");
        sb.append("    \"totalIncidents\": ").append(aggregator.getTotalIncidentCount()).append("\n");
        sb.append("  },\n");

        sb.append("  \"anomalyBreakdown\": {\n");
        Map<AnomalyType, Integer> typeCounts = aggregator.getAnomalyTypeCounts();
        int idx = 0;
        for (Map.Entry<AnomalyType, Integer> entry : typeCounts.entrySet()) {
            sb.append("    \"").append(entry.getKey().name()).append("\": ").append(entry.getValue());
            if (++idx < typeCounts.size()) sb.append(",");
            sb.append("\n");
        }
        sb.append("  },\n");

        sb.append("  \"topOffenders\": [\n");
        List<IncidentAggregator.IpOffenseSummary> offenders = aggregator.getTopOffenders(10);
        for (int i = 0; i < offenders.size(); i++) {
            IncidentAggregator.IpOffenseSummary off = offenders.get(i);
            sb.append("    {\n");
            sb.append("      \"ip\": \"").append(escape(off.ip())).append("\",\n");
            sb.append("      \"incidentCount\": ").append(off.incidentCount()).append(",\n");
            sb.append("      \"maxSeverity\": \"").append(off.maxSeverity().name()).append("\"\n");
            sb.append("    }").append(i < offenders.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ],\n");

        sb.append("  \"incidents\": [\n");
        List<Incident> incidents = aggregator.getAllIncidents();
        for (int i = 0; i < incidents.size(); i++) {
            Incident inc = incidents.get(i);
            sb.append("    {\n");
            sb.append("      \"id\": \"").append(inc.getIncidentId()).append("\",\n");
            sb.append("      \"type\": \"").append(inc.getAnomalyType().name()).append("\",\n");
            sb.append("      \"severity\": \"").append(inc.getSeverity().name()).append("\",\n");
            sb.append("      \"clientIp\": \"").append(escape(inc.getClientIp())).append("\",\n");
            sb.append("      \"detectedAt\": \"").append(inc.getDetectedAt()).append("\",\n");
            sb.append("      \"details\": \"").append(escape(inc.getDetails())).append("\"\n");
            sb.append("    }").append(i < incidents.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write(sb.toString());
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public String getFormatName() {
        return "JSON";
    }
}
