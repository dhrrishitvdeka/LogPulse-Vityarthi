package com.logpulse.engine;

import com.logpulse.model.Incident;
import com.logpulse.model.LogEntry;

import java.util.Optional;

public interface Rule {
    Optional<Incident> evaluate(LogEntry entry);
    String getRuleName();
}
