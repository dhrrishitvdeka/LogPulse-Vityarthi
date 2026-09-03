package com.logpulse.exception;

/**
 * Thrown when command-line arguments or system configurations are missing, invalid, or conflicting.
 */
public class ConfigurationException extends LogPulseException {
    public ConfigurationException(String message) {
        super(message);
    }
}
