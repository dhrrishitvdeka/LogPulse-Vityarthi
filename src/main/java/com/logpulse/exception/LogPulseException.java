package com.logpulse.exception;

public class LogPulseException extends RuntimeException {
    public LogPulseException(String message) {
        super(message);
    }

    public LogPulseException(String message, Throwable cause) {
        super(message, cause);
    }
}
