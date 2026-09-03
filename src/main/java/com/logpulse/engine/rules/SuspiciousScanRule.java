package com.logpulse.engine.rules;

import com.logpulse.engine.Rule;
import com.logpulse.model.AnomalyType;
import com.logpulse.model.Incident;
import com.logpulse.model.LogEntry;
import com.logpulse.model.SeverityLevel;

import java.util.List;
import java.util.Optional;

/**
 * Detects malicious endpoint reconnaissance, web vulnerability scanners, and directory traversal probes.
 */
public class SuspiciousScanRule implements Rule {

    private static final List<String> SENSITIVE_PATTERNS = List.of(
            "/wp-admin",
            "/wp-login",
            "/.env",
            "/.git",
            "/phpmyadmin",
            "/actuator",
            "/etc/passwd",
            "/server-status",
            "/cgi-bin/",
            "/swagger-ui",
            "/api/v1/debug",
            "../",
            "..%2f"
    );

    @Override
    public Optional<Incident> evaluate(LogEntry entry) {
        String endpointLower = entry.getEndpoint().toLowerCase();

        for (String pattern : SENSITIVE_PATTERNS) {
            if (endpointLower.contains(pattern)) {
                String details = String.format("Reconnaissance signature '%s' detected in request URL '%s'",
                        pattern, entry.getEndpoint());

                return Optional.of(new Incident(
                        AnomalyType.SUSPICIOUS_PATH_SCAN,
                        SeverityLevel.HIGH,
                        entry.getClientIp(),
                        entry.getTimestamp(),
                        1,
                        0,
                        details
                ));
            }
        }

        return Optional.empty();
    }

    @Override
    public String getRuleName() {
        return "SuspiciousPathReconnaissanceRule";
    }
}
