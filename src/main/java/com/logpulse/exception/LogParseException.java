package com.logpulse.exception;

public class LogParseException extends LogPulseException {
    private final String rawLine;
    private final long lineNumber;

    public LogParseException(String message, String rawLine, long lineNumber) {
        super("Line " + lineNumber + ": " + message + " [" + rawLine + "]");
        this.rawLine = rawLine;
        this.lineNumber = lineNumber;
    }

    public LogParseException(String message, String rawLine, long lineNumber, Throwable cause) {
        super("Line " + lineNumber + ": " + message + " [" + rawLine + "]", cause);
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
