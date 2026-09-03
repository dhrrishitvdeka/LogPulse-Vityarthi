package com.logpulse.engine.rules;

import com.logpulse.engine.Rule;
import com.logpulse.model.AnomalyType;
import com.logpulse.model.Incident;
import com.logpulse.model.LogEntry;
import com.logpulse.model.SeverityLevel;

import java.util.List;
import java.util.Optional;

public class SuspiciousScanRule implements Rule {

    private static final List<String> PATHS = List.of(
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
        String path = entry.getEndpoint().toLowerCase();

        for (String target : PATHS) {
            if (path.contains(target)) {
                return Optional.of(new Incident(
                        AnomalyType.SUSPICIOUS_PATH_SCAN,
                        SeverityLevel.HIGH,
                        entry.getClientIp(),
                        entry.getTimestamp(),
                        1,
                        0,
                        "Matched pattern: " + target
                ));
            }
        }

        return Optional.empty();
    }

    @Override
    public String getRuleName() {
        return "SuspiciousScanRule";
    }
}
