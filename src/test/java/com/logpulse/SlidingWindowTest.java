package com.logpulse;

import com.logpulse.engine.SlidingWindowRateLimiter;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Unit tests for sliding-window rate tracking algorithm and concurrency safety.
 */
public class SlidingWindowTest {

    public static void runAll() {
        testBasicWindowCounting();
        testWindowEviction();
        testConcurrentRateLimiter();
        System.out.println("  ✔ SlidingWindowTest: All sliding-window test suites passed.");
    }

    public static void testBasicWindowCounting() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(60);
        Instant now = Instant.now();

        int c1 = limiter.recordAndCount("192.168.1.1", now);
        int c2 = limiter.recordAndCount("192.168.1.1", now.plusSeconds(5));
        int c3 = limiter.recordAndCount("192.168.1.1", now.plusSeconds(10));

        assert c1 == 1 : "Expected 1";
        assert c2 == 2 : "Expected 2";
        assert c3 == 3 : "Expected 3";
    }

    public static void testWindowEviction() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(10);
        Instant t0 = Instant.parse("2026-09-03T10:00:00Z");

        limiter.recordAndCount("test-ip", t0);
        limiter.recordAndCount("test-ip", t0.plusSeconds(3));

        // Advance beyond the 10-second window
        int countAfterWindow = limiter.recordAndCount("test-ip", t0.plusSeconds(12));

        // t0 (0s) should have been evicted; only t0+3s and t0+12s are within 10s of t0+12s
        assert countAfterWindow == 2 : "Expected 2 after oldest event eviction, got " + countAfterWindow;
    }

    public static void testConcurrentRateLimiter() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(60);
        int threadCount = 8;
        int requestsPerThread = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        Instant baseTime = Instant.now();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
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
        int expected = threadCount * requestsPerThread;
        assert finalCount == expected : "Expected " + expected + " under concurrent execution, got " + finalCount;
    }
}
