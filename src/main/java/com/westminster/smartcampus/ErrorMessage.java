package com.westminster.smartcampus;

/**
 * Simple JSON-friendly error wrapper used by resources and exception
 * mappers to return a consistent { "error": "..." } structure.
 *
 * Keeping a tiny class (instead of returning raw strings or Maps)
 * gives Jackson a clean target type and makes the API's error
 * shape predictable for clients.
 */
public class ErrorMessage {

    private String error;

    public ErrorMessage() {
    }

    public ErrorMessage(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}