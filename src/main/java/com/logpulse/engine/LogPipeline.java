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
import java.util.concurrent.atomic.AtomicReference;

public class LogPipeline {

    private static final String EOF_MARKER = new String("__LOGPULSE_POISON_PILL__");

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
                        if (line == EOF_MARKER) {
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

        AtomicReference<Throwable> producerError = new AtomicReference<>();
        CompletableFuture<Void> producer = CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stats.incrementLinesRead();
                    stats.addBytes(line.length() + 1);
                    queue.put(line);
                }
            } catch (IOException | InterruptedException e) {
                producerError.set(e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                for (int i = 0; i < threads; i++) {
                    try {
                        queue.put(EOF_MARKER);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });

        try {
            producer.join();
            if (producerError.get() != null) {
                throw new LogPulseException("Error reading input log file: " + producerError.get().getMessage(), producerError.get());
            }
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
