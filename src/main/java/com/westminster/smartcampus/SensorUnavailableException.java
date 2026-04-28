package com.westminster.smartcampus;

/**
 * Thrown when an action is attempted on a sensor whose state
 * forbids it — currently, posting a reading to a sensor marked
 * as MAINTENANCE. Mapped to HTTP 403 Forbidden.
 */
public class SensorUnavailableException extends RuntimeException {

    public SensorUnavailableException(String message) {
        super(message);
    }
}