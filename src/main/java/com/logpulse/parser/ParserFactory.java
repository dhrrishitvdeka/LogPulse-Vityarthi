package com.logpulse.parser;

import com.logpulse.exception.ConfigurationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ParserFactory {

    private static final List<LogParser> PARSERS = List.of(
            new JsonLogParser(),
            new ApacheCombinedLogParser(),
            new SyslogParser()
    );

    private ParserFactory() {}

    public static LogParser getParser(String format, Path filePath) {
        if (format == null || format.isBlank() || "auto".equalsIgnoreCase(format)) {
            return autoDetect(filePath);
        }

        return switch (format.toLowerCase()) {
            case "apache", "nginx", "combined", "clf" -> new ApacheCombinedLogParser();
            case "json", "ndjson" -> new JsonLogParser();
            case "syslog", "rfc5424" -> new SyslogParser();
            default -> throw new ConfigurationException("Unsupported log format: " + format);
        };
    }

    public static LogParser autoDetect(Path filePath) {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                for (LogParser parser : PARSERS) {
                    if (parser.canParse(line)) {
                        return parser;
                    }
                }
                break;
            }
        } catch (IOException e) {
            throw new ConfigurationException("Unable to read file for format detection: " + e.getMessage());
        }

        return new ApacheCombinedLogParser();
    }
}
