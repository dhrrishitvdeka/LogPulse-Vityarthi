package com.logpulse.engine;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindowRateLimiter {

    private final ConcurrentHashMap<String, ArrayDeque<Long>> keyWindows = new ConcurrentHashMap<>();
    private final long windowMillis;

    public SlidingWindowRateLimiter(long windowSeconds) {
        this.windowMillis = windowSeconds * 1000L;
    }

    public int recordAndCount(String key, Instant eventTime) {
        long currentTimestamp = (eventTime != null) ? eventTime.toEpochMilli() : System.currentTimeMillis();
        long cutoff = currentTimestamp - windowMillis;

        ArrayDeque<Long> window = keyWindows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst() < cutoff) {
                window.pollFirst();
            }
            window.addLast(currentTimestamp);
            return window.size();
        }
    }

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
