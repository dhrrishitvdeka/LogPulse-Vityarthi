package com.logpulse.reporter;

import com.logpulse.aggregator.IncidentAggregator;
import com.logpulse.model.LogStats;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Strategy interface for persisting audit reports to secondary storage.
 */
public interface ReportExporter {

    /**
     * Serializes aggregated statistics and incident records to the specified destination path.
     *
     * @param stats      Pipeline telemetry data.
     * @param aggregator Incident aggregates.
     * @param outputPath Target file path.
     * @throws IOException If file creation or writing fails.
     */
    void export(LogStats stats, IncidentAggregator aggregator, Path outputPath) throws IOException;

    String getFormatName();
}
