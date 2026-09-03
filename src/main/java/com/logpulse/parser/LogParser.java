package com.logpulse.parser;

import com.logpulse.exception.LogParseException;
import com.logpulse.model.LogEntry;

/**
 * Strategy interface defining the contract for parsing individual raw log strings.
 */
public interface LogParser {

    /**
     * Parses a single raw log line into an immutable {@link LogEntry}.
     *
     * @param rawLine    The unparsed log line string.
     * @param lineNumber The sequential line number in the source file.
     * @return Fully populated LogEntry.
     * @throws LogParseException if the line does not match the expected format.
     */
    LogEntry parse(String rawLine, long lineNumber) throws LogParseException;

    /**
     * Determines whether this parser can process the given sample line.
     *
     * @param sampleLine A preview line from the file.
     * @return true if compatible, false otherwise.
     */
    boolean canParse(String sampleLine);

    /**
     * Human-readable identifier of the log format.
     */
    String getFormatName();
}
