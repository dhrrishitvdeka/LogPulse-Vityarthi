package com.logpulse.reporter;

import com.logpulse.aggregator.IncidentAggregator;
import com.logpulse.model.Incident;
import com.logpulse.model.LogStats;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Exports incidents in tabular CSV format for SIEM ingestion and spreadsheet analysis.
 */
public class CsvReportExporter implements ReportExporter {

    @Override
    public void export(LogStats stats, IncidentAggregator aggregator, Path outputPath) throws IOException {
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            // Write CSV Header
            writer.write("IncidentId,Timestamp,AnomalyType,Severity,ClientIP,EventCount,WindowSeconds,Details\n");

            List<Incident> incidents = aggregator.getAllIncidents();
            for (Incident inc : incidents) {
                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,%d,\"%s\"\n",
                        inc.getIncidentId(),
                        inc.getDetectedAt(),
                        inc.getAnomalyType().name(),
                        inc.getSeverity().name(),
                        inc.getClientIp(),
                        inc.getEventCount(),
                        inc.getWindowSeconds(),
                        inc.getDetails().replace("\"", "\"\"")));
            }
        }
    }

    @Override
    public String getFormatName() {
        return "CSV";
    }
}
