package com.logpulse.parser;

import com.logpulse.exception.ConfigurationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Factory class providing parser strategy resolution and format auto-detection.
 */
public final class ParserFactory {

    private static final List<LogParser> AVAILABLE_PARSERS = List.of(
            new JsonLogParser(),
            new ApacheCombinedLogParser(),
            new SyslogParser()
    );

    private ParserFactory() {
        // Prevent instantiation
    }

    /**
     * Resolves the appropriate LogParser strategy based on requested format string or auto-detection.
     *
     * @param format   "auto", "apache", "nginx", "json", "syslog"
     * @param filePath Path to the target log file for sample sniffing when "auto" is requested.
     * @return Resolved LogParser instance.
     */
    public static LogParser getParser(String format, Path filePath) {
        if (format == null || format.isBlank() || format.equalsIgnoreCase("auto")) {
            return autoDetect(filePath);
        }

        return switch (format.toLowerCase()) {
            case "apache", "nginx", "combined", "clf" -> new ApacheCombinedLogParser();
            case "json", "ndjson" -> new JsonLogParser();
            case "syslog", "rfc5424" -> new SyslogParser();
            default -> throw new ConfigurationException("Unsupported log format: '" + format +
                    "'. Supported formats: auto, apache, nginx, json, syslog");
        };
    }

    /**
     * Reads the first non-empty line of the file and inspects patterns to detect format.
     */
    public static LogParser autoDetect(Path filePath) {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String sampleLine;
            while ((sampleLine = reader.readLine()) != null) {
                if (sampleLine.isBlank() || sampleLine.startsWith("#")) {
                    continue;
                }
                for (LogParser parser : AVAILABLE_PARSERS) {
                    if (parser.canParse(sampleLine)) {
                        return parser;
                    }
                }
                break;
            }
        } catch (IOException e) {
            throw new ConfigurationException("Unable to read sample line for format detection: " + e.getMessage());
        }

        // Default fallback to Apache Combined format
        return new ApacheCombinedLogParser();
    }
}
