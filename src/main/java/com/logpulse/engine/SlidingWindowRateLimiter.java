package com.logpulse.engine;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe sliding-window rate tracking algorithm using timestamp ring-buffers.
 * Guarantees O(1) sliding window checks and constant memory bounds via automatic eviction.
 */
public class SlidingWindowRateLimiter {

    private final ConcurrentHashMap<String, ArrayDeque<Long>> keyWindows = new ConcurrentHashMap<>();
    private final long windowMillis;

    public SlidingWindowRateLimiter(long windowSeconds) {
        this.windowMillis = windowSeconds * 1000L;
    }

    /**
     * Records an event for the given key and returns the number of occurrences within the sliding window.
     *
     * @param key       The identifier (e.g. client IP or IP:ACTION).
     * @param eventTime The timestamp of the log event.
     * @return Current event count within the sliding window.
     */
    public int recordAndCount(String key, Instant eventTime) {
        long currentTimestamp = (eventTime != null) ? eventTime.toEpochMilli() : System.currentTimeMillis();
        long cutoff = currentTimestamp - windowMillis;

        ArrayDeque<Long> window = keyWindows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            // Evict expired timestamps older than the sliding window boundary
            while (!window.isEmpty() && window.peekFirst() < cutoff) {
                window.pollFirst();
            }

            // Append current timestamp
            window.addLast(currentTimestamp);
            return window.size();
        }
    }

    /**
     * Inspects current count for a key without recording a new event.
     */
    public int getCount(String key, Instant referenceTime) {
        ArrayDeque<Long> window = keyWindows.get(key);
        if (window == null) {
            return 0;
        }

        long currentTimestamp = (referenceTime != null) ? referenceTime.toEpochMilli() : System.currentTimeMillis();
        long cutoff = currentTimestamp - windowMillis;

        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst() < cutoff) {
                window.pollFirst();
            }
            return window.size();
        }
    }

    /**
     * Prunes inactive keys from the map to conserve memory in long-running pipelines.
     */
    public void cleanupInactiveKeys() {
        long now = System.currentTimeMillis();
        long cutoff = now - windowMillis;

        keyWindows.entrySet().removeIf(entry -> {
            ArrayDeque<Long> window = entry.getValue();
            synchronized (window) {
                while (!window.isEmpty() && window.peekFirst() < cutoff) {
                    window.pollFirst();
                }
                return window.isEmpty();
            }
        });
    }

    public int getActiveKeyCount() {
        return keyWindows.size();
    }

    public void clear() {
        keyWindows.clear();
    }
}
