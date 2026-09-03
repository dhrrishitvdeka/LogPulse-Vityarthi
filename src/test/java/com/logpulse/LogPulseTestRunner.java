package com.logpulse;

/**
 * Self-contained CLI Test Runner that runs the full test suite without external dependencies.
 */
public class LogPulseTestRunner {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("LOGPULSE TEST SUITE: RUNNING AUTOMATED UNIT & INTEGRATION VERIFICATION");
        System.out.println("================================================================================");

        long start = System.currentTimeMillis();
        int passedSuites = 0;

        try {
            System.out.println("\n[1/4] Running LogParser Tests...");
            ParserTest.runAll();
            passedSuites++;

            System.out.println("\n[2/4] Running SlidingWindow Rate Limiter & Concurrency Tests...");
            SlidingWindowTest.runAll();
            passedSuites++;

            System.out.println("\n[3/4] Running Anomaly Detection Rule Tests...");
            AnomalyDetectionTest.runAll();
            passedSuites++;

            System.out.println("\n[4/4] Running Pipeline Concurrency & Top-K Heap Tests...");
            PipelineConcurrencyTest.runAll();
            passedSuites++;

            long duration = System.currentTimeMillis() - start;
            System.out.println("\n================================================================================");
            System.out.printf("ALL TEST SUITES PASSED (%d/%d) in %d ms.%n", passedSuites, 4, duration);
            System.out.println("Exit Code: 0 (OK)");
            System.out.println("================================================================================");
            System.exit(0);

        } catch (Throwable t) {
            System.err.println("\n\u001B[31m[TEST FAILURE] " + t.getMessage() + "\u001B[0m");
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
