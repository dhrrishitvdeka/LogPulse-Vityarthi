package com.logpulse;

import com.logpulse.engine.SlidingWindowRateLimiter;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SlidingWindowTest {

    public static void runAll() {
        testBasicCounting();
        testEviction();
        testConcurrency();
    }

    public static void testBasicCounting() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(60);
        Instant now = Instant.now();

        int c1 = limiter.recordAndCount("192.168.1.1", now);
        int c2 = limiter.recordAndCount("192.168.1.1", now.plusSeconds(5));
        int c3 = limiter.recordAndCount("192.168.1.1", now.plusSeconds(10));

        if (c1 != 1 || c2 != 2 || c3 != 3) {
            throw new AssertionError("Counter error: " + c1 + ", " + c2 + ", " + c3);
        }
    }

    public static void testEviction() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(10);
        Instant t0 = Instant.parse("2026-09-03T10:00:00Z");

        limiter.recordAndCount("test-ip", t0);
        limiter.recordAndCount("test-ip", t0.plusSeconds(3));

        int count = limiter.recordAndCount("test-ip", t0.plusSeconds(12));
        if (count != 2) {
            throw new AssertionError("Expected 2 after window eviction, got " + count);
        }
    }

    public static void testConcurrency() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(60);
        int threads = 6;
        int reqsPerThread = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        Instant baseTime = Instant.now();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    for (int j = 0; j < reqsPerThread; j++) {
                        limiter.recordAndCount("shared-ip", baseTime);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
            pool.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int finalCount = limiter.getCount("shared-ip", baseTime);
        int expected = threads * reqsPerThread;
        if (finalCount != expected) {
            throw new AssertionError("Expected " + expected + ", got " + finalCount);
        }
    }
}
