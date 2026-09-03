package com.logpulse;

import com.logpulse.config.LogPulseConfig;
import com.logpulse.engine.LogPipeline;
import com.logpulse.exception.ConfigurationException;
import com.logpulse.model.LogStats;
import com.logpulse.reporter.CsvReportExporter;
import com.logpulse.reporter.JsonReportExporter;
import com.logpulse.reporter.TerminalReporter;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Command-line entrypoint for the LogPulse Anomaly & Rate Limiter Engine.
 */
public class Main {

    public static void main(String[] args) {
        if (args == null || args.length == 0 || hasFlag(args, "--help", "-h")) {
            printHelp();
            System.exit(0);
        }

        try {
            LogPulseConfig config = parseCommandLineArgs(args);
            System.out.println("Initializing LogPulse Engine on: " + config.getLogFilePath() + "...");
            System.out.printf("Configuration: Worker Threads = %d | Format = %s | Window = %ds%n",
                    config.getWorkerThreads(), config.getFormat(), config.getSlidingWindowSeconds());

            // Instantiate and execute multi-threaded pipeline
            LogPipeline pipeline = new LogPipeline(config);
            LogStats stats = pipeline.execute();

            // Render rich terminal dashboard
            TerminalReporter reporter = new TerminalReporter();
            reporter.renderDashboard(stats, pipeline.getAggregator(), config.getTopKOffenders());

            // Handle file exports if requested
            handleExports(config, stats, pipeline);

            System.exit(0);

        } catch (ConfigurationException e) {
            System.err.println("\u001B[31m[Configuration Error] " + e.getMessage() + "\u001B[0m");
            System.err.println("Run with --help to see all available CLI options.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("\u001B[31m[Execution Failure] " + e.getMessage() + "\u001B[0m");
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static LogPulseConfig parseCommandLineArgs(String[] args) {
        LogPulseConfig.Builder builder = LogPulseConfig.builder();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--") && i + 1 < args.length) {
                String val = args[++i];
                switch (arg) {
                    case "--file", "-f" -> builder.logFilePath(val);
                    case "--format" -> builder.format(val);
                    case "--window", "-w" -> builder.slidingWindowSeconds(Long.parseLong(val));
                    case "--rate-limit", "-r" -> builder.rateLimitThreshold(Integer.parseInt(val));
                    case "--auth-threshold", "-a" -> builder.authFailureThreshold(Integer.parseInt(val));
                    case "--error-threshold", "-e" -> builder.serverErrorThreshold(Integer.parseInt(val));
                    case "--top", "-k" -> builder.topKOffenders(Integer.parseInt(val));
                    case "--threads", "-t" -> builder.workerThreads(Integer.parseInt(val));
                    case "--export" -> builder.exportFormat(val);
                    case "--output", "-o" -> builder.exportPath(val);
                    default -> throw new ConfigurationException("Unknown CLI option: " + arg);
                }
            } else if (arg.startsWith("--")) {
                throw new ConfigurationException("Option " + arg + " requires a following value argument.");
            }
        }

        return builder.build();
    }

    private static void handleExports(LogPulseConfig config, LogStats stats, LogPipeline pipeline) {
        String exportFormat = config.getExportFormat();
        if ("none".equalsIgnoreCase(exportFormat)) {
            return;
        }

        try {
            Path targetPath = Paths.get(config.getExportPath());
            if ("json".equalsIgnoreCase(exportFormat)) {
                new JsonReportExporter().export(stats, pipeline.getAggregator(), targetPath);
                System.out.println("✔ Exported JSON audit report to: " + targetPath.toAbsolutePath());
            } else if ("csv".equalsIgnoreCase(exportFormat)) {
                new CsvReportExporter().export(stats, pipeline.getAggregator(), targetPath);
                System.out.println("✔ Exported CSV audit report to: " + targetPath.toAbsolutePath());
            } else if ("all".equalsIgnoreCase(exportFormat)) {
                Path jsonPath = Paths.get(targetPath.toString() + ".json");
                Path csvPath = Paths.get(targetPath.toString() + ".csv");
                new JsonReportExporter().export(stats, pipeline.getAggregator(), jsonPath);
                new CsvReportExporter().export(stats, pipeline.getAggregator(), csvPath);
                System.out.println("✔ Exported JSON report to: " + jsonPath.toAbsolutePath());
                System.out.println("✔ Exported CSV report to:  " + csvPath.toAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("\u001B[33m[Warning] Failed to generate export report: " + e.getMessage() + "\u001B[0m");
        }
    }

    private static boolean hasFlag(String[] args, String... flags) {
        for (String arg : args) {
            for (String flag : flags) {
                if (arg.equalsIgnoreCase(flag)) return true;
            }
        }
        return false;
    }

    private static void printHelp() {
        System.out.println("""
            ================================================================================
            LogPulse: Multi-Threaded Server Log Anomaly & Rate Limiter Engine
            ================================================================================
            Usage:
              java -cp <classpath> com.logpulse.Main --file <path> [options]

            Required Options:
              --file, -f <path>             Target server log file path.

            Execution Options:
              --format <type>               Parser strategy: 'auto', 'apache', 'json', 'syslog'
                                            (default: auto)
              --window, -w <seconds>        Sliding-window duration in seconds (default: 60)
              --rate-limit, -r <count>      Requests per window before rate limit alert (default: 50)
              --auth-threshold, -a <count>  Consecutive 401/403 failures for brute-force alert (default: 5)
              --error-threshold, -e <count> 5xx internal errors before burst alert (default: 10)
              --top, -k <number>            Number of top offending IPs to display (default: 5)
              --threads, -t <number>        Worker thread count (default: available CPU cores)

            Export Options:
              --export <format>             Persistence format: 'none', 'json', 'csv', 'all'
                                            (default: none)
              --output, -o <path>           Destination path for exported report file

            General:
              --help, -h                    Display this help message and exit.
            ================================================================================
            """);
    }
}
