package com.logpulse.reporter;

import com.logpulse.aggregator.IncidentAggregator;
import com.logpulse.model.LogStats;

import java.io.IOException;
import java.nio.file.Path;

public interface ReportExporter {
    void export(LogStats stats, IncidentAggregator aggregator, Path outputPath) throws IOException;
    String getFormatName();
}
