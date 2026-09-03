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

/**
 * Multi-threaded Producer-Consumer pipeline orchestrating concurrent log parsing
 * and real-time anomaly evaluation using bounded BlockingQueues.
 */
public class LogPipeline {

    private static final String POISON_PILL = "__LOGPULSE_STREAM_EOF_TOKEN__";

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

        Path logPath = Paths.get(config.getLogFilePath());
        this.parser = ParserFactory.getParser(config.getFormat(), logPath);
    }

    /**
     * Executes the streaming pipeline synchronously and returns collected metrics.
     */
    public LogStats execute() {
        Path filePath = Paths.get(config.getLogFilePath());
        int threadCount = config.getWorkerThreads();
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(config.getQueueCapacity());
        ExecutorService workerPool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicLong lineCounter = new AtomicLong(0);

        // 1. Submit Consumer Workers
        for (int i = 0; i < threadCount; i++) {
            workerPool.submit(() -> {
                try {
                    while (true) {
                        String rawLine = queue.take();
                        if (POISON_PILL.equals(rawLine)) {
                            break;
                        }

                        long lineNum = lineCounter.incrementAndGet();
                        processLine(rawLine, lineNum);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 2. Start Producer on separate thread to feed the bounded queue
        CompletableFuture<Void> producerFuture = CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stats.incrementLinesRead();
                    stats.addBytes(line.length() + 1); // approximate bytes with newline
                    queue.put(line);
                }

                // Append poison pills to gracefully notify all worker threads
                for (int i = 0; i < threadCount; i++) {
                    queue.put(POISON_PILL);
                }
            } catch (IOException | InterruptedException e) {
                throw new LogPulseException("I/O error during log ingestion: " + e.getMessage(), e);
            }
        });

        // 3. Await completion of producer and consumer workers
        try {
            producerFuture.join();
            latch.await();
            workerPool.shutdown();
            if (!workerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LogPulseException("Pipeline interrupted during execution", e);
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
            for (Incident incident : incidents) {
                aggregator.record(incident);
                stats.incrementIncidents();
            }
        } catch (LogParseException e) {
            // Fault tolerance: record malformed line without crashing pipeline
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
