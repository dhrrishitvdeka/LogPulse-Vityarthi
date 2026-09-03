package com.logpulse.exception;

/**
 * Thrown when a raw log line cannot be parsed according to the configured format strategy.
 */
public class LogParseException extends LogPulseException {
    private final String rawLine;
    private final long lineNumber;

    public LogParseException(String message, String rawLine, long lineNumber) {
        super(String.format("Error on line %d: %s [Line: %s]", lineNumber, message, rawLine));
        this.rawLine = rawLine;
        this.lineNumber = lineNumber;
    }

    public LogParseException(String message, String rawLine, long lineNumber, Throwable cause) {
        super(String.format("Error on line %d: %s [Line: %s]", lineNumber, message, rawLine), cause);
        this.rawLine = rawLine;
        this.lineNumber = lineNumber;
    }

    public String getRawLine() {
        return rawLine;
    }

    public long getLineNumber() {
        return lineNumber;
    }
}
