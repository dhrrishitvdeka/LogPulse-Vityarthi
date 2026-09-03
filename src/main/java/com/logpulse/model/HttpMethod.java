package com.logpulse.model;

/**
 * Standard HTTP Request Methods supported by web servers.
 */
public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS,
    TRACE,
    CONNECT,
    UNKNOWN;

    public static HttpMethod fromString(String methodStr) {
        if (methodStr == null || methodStr.isBlank()) {
            return UNKNOWN;
        }
        try {
            return HttpMethod.valueOf(methodStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
