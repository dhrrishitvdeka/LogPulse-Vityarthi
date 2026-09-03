package com.logpulse.exception;

/**
 * Base unchecked exception for all runtime errors within the LogPulse engine.
 */
public class LogPulseException extends RuntimeException {
    public LogPulseException(String message) {
        super(message);
    }

    public LogPulseException(String message, Throwable cause) {
        super(message, cause);
    }
}
