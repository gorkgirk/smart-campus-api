package com.westminster.smartcampus;

/**
 * Thrown when a client attempts to delete a Room that still has
 * sensors assigned to it. Mapped to HTTP 409 Conflict by
 * RoomNotEmptyExceptionMapper.
 *
 * Extends RuntimeException so resource methods don't need to declare
 * it in throws clauses — JAX-RS will catch it as it bubbles up.
 */
public class RoomNotEmptyException extends RuntimeException {

    public RoomNotEmptyException(String message) {
        super(message);
    }
}