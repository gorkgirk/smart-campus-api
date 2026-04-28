package com.westminster.smartcampus;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps LinkedResourceNotFoundException to HTTP 422 Unprocessable Entity.
 *
 * 422 is taken straight from RFC 4918 — "the request was well-formed
 * but was unable to be followed due to semantic errors." Perfect fit
 * for "I understand your request, the JSON is valid, but the thing
 * you referenced doesn't exist."
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    // 422 isn't in jakarta.ws.rs.core.Response.Status, so we use the int directly.
    private static final int UNPROCESSABLE_ENTITY = 422;

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        return Response.status(UNPROCESSABLE_ENTITY)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorMessage(ex.getMessage()))
                .build();
    }
}