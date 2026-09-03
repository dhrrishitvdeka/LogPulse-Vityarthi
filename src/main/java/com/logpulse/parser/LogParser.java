package com.logpulse.parser;

import com.logpulse.exception.LogParseException;
import com.logpulse.model.LogEntry;

public interface LogParser {
    LogEntry parse(String rawLine, long lineNumber) throws LogParseException;
    boolean canParse(String sampleLine);
    String getFormatName();
}
