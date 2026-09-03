package com.logpulse.engine;

import com.logpulse.aggregator.IncidentAggregator;
import com.logpulse.config.LogPulseConfig;
import com.logpulse.exception.LogParseException;
import com.logpulse.exception.LogPulseException;
import com.logpulse.model.Incident;
import com.logpulse.model.LogEntry;
import com.logpulse.model.LogStats;
import com.logpulse.parser.LogParser;
import com.logpulse.parser.ParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class LogPipeline {

    private static final String EOF_MARKER = "__EOF__";

    private final LogPulseConfig config;
    private final LogStats stats;
    private final IncidentAggregator aggregator;
    private final AnomalyDetectionEngine detectionEngine;
    private final LogParser parser;

    public LogPipeline(LogPulseConfig config) {
        this.config = config;
        this.stats = new LogStats();
        this.aggregator = new IncidentAggregator();
        this.detectionEngine = new AnomalyDetectionEngine(config);

        Path path = Paths.get(config.getLogFilePath());
        this.parser = ParserFactory.getParser(config.getFormat(), path);
    }

    public LogStats execute() {
        Path filePath = Paths.get(config.getLogFilePath());
        int threads = config.getWorkerThreads();
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(config.getQueueCapacity());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicLong lineCounter = new AtomicLong(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    while (true) {
                        String line = queue.take();
                        if (EOF_MARKER.equals(line)) {
                            break;
                        }
                        processLine(line, lineCounter.incrementAndGet());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        CompletableFuture<Void> producer = CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stats.incrementLinesRead();
                    stats.addBytes(line.length() + 1);
                    queue.put(line);
                }
                for (int i = 0; i < threads; i++) {
                    queue.put(EOF_MARKER);
                }
            } catch (IOException | InterruptedException e) {
                throw new LogPulseException("Error reading input log file: " + e.getMessage(), e);
            }
        });

        try {
            producer.join();
            latch.await();
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LogPulseException("Pipeline processing interrupted", e);
        } finally {
            stats.finish();
        }

        return stats;
    }

    private void processLine(String rawLine, long lineNum) {
        if (rawLine.isBlank() || rawLine.startsWith("#")) {
            return;
        }

        try {
            LogEntry entry = parser.parse(rawLine, lineNum);
            stats.incrementValidParsed();
            stats.recordStatusCode(entry.getStatusCode());

            List<Incident> incidents = detectionEngine.evaluate(entry);
            for (Incident inc : incidents) {
                aggregator.record(inc);
                stats.incrementIncidents();
            }
        } catch (LogParseException e) {
            stats.incrementMalformed();
        } catch (Exception e) {
            stats.incrementMalformed();
        }
    }

    public LogStats getStats() {
        return stats;
    }

    public IncidentAggregator getAggregator() {
        return aggregator;
    }

    public LogParser getParser() {
        return parser;
    }

    public LogPulseConfig getConfig() {
        return config;
    }
}
