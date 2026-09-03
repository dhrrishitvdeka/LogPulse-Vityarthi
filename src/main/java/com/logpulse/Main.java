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

public class Main {

    public static void main(String[] args) {
        if (args == null || args.length == 0 || hasFlag(args, "--help", "-h")) {
            printHelp();
            System.exit(0);
        }

        try {
            LogPulseConfig config = parseCommandLineArgs(args);
            System.out.println("Processing: " + config.getLogFilePath());
            System.out.println("Workers: " + config.getWorkerThreads() + " | Format: " + config.getFormat());

            LogPipeline pipeline = new LogPipeline(config);
            LogStats stats = pipeline.execute();

            TerminalReporter reporter = new TerminalReporter();
            reporter.renderDashboard(stats, pipeline.getAggregator(), config.getTopKOffenders());

            handleExports(config, stats, pipeline);
            System.exit(0);

        } catch (ConfigurationException e) {
            System.err.println("Config error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Execution failed: " + e.getMessage());
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
                    default -> throw new ConfigurationException("Unknown option: " + arg);
                }
            } else if (arg.startsWith("--")) {
                throw new ConfigurationException("Missing argument value for " + arg);
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
                System.out.println("Saved JSON report to: " + targetPath.toAbsolutePath());
            } else if ("csv".equalsIgnoreCase(exportFormat)) {
                new CsvReportExporter().export(stats, pipeline.getAggregator(), targetPath);
                System.out.println("Saved CSV report to: " + targetPath.toAbsolutePath());
            } else if ("all".equalsIgnoreCase(exportFormat)) {
                Path jsonPath = Paths.get(targetPath.toString() + ".json");
                Path csvPath = Paths.get(targetPath.toString() + ".csv");
                new JsonReportExporter().export(stats, pipeline.getAggregator(), jsonPath);
                new CsvReportExporter().export(stats, pipeline.getAggregator(), csvPath);
                System.out.println("Saved JSON report to: " + jsonPath.toAbsolutePath());
                System.out.println("Saved CSV report to: " + csvPath.toAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Export warning: " + e.getMessage());
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
            Usage: java -cp <classpath> com.logpulse.Main --file <path> [options]

            Options:
              --file, -f <path>             Input log file path
              --format <type>               Log format (auto, apache, json, syslog)
              --window, -w <seconds>        Sliding window in seconds (default: 60)
              --rate-limit, -r <count>      Requests per window before rate alert (default: 50)
              --auth-threshold, -a <count>  401/403 failures before brute force alert (default: 5)
              --error-threshold, -e <count> 5xx errors before burst alert (default: 10)
              --top, -k <number>            Top N offending IPs (default: 5)
              --threads, -t <number>        Worker threads (default: CPU cores)
              --export <format>             Export format (none, json, csv, all)
              --output, -o <path>           Destination path for exported report
              --help, -h                    Show help message
            """);
    }
}
