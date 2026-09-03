package com.logpulse;

public class LogPulseTestRunner {

    public static void main(String[] args) {
        System.out.println("Running LogPulse test suite...");
        long start = System.currentTimeMillis();

        try {
            ParserTest.runAll();
            System.out.println("  [OK] Parser tests passed");

            SlidingWindowTest.runAll();
            System.out.println("  [OK] Sliding window tests passed");

            AnomalyDetectionTest.runAll();
            System.out.println("  [OK] Anomaly detection tests passed");

            PipelineConcurrencyTest.runAll();
            System.out.println("  [OK] Concurrency & heap tests passed");

            long duration = System.currentTimeMillis() - start;
            System.out.println("All tests passed (" + duration + " ms).");
            System.exit(0);

        } catch (Throwable t) {
            System.err.println("Test failed: " + t.getMessage());
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
