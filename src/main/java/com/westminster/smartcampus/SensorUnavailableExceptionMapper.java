package com.westminster.smartcampus;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps SensorUnavailableException to HTTP 403 Forbidden.
 *
 * The sensor exists and the request is well-formed, but the
 * sensor's state (MAINTENANCE / OFFLINE) forbids the action.
 * 403 is the right code: "I understood you and refuse to do it
 * because of the resource's current state."
 */
@Provider
public class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException ex) {
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorMessage(ex.getMessage()))
                .build();
    }
}