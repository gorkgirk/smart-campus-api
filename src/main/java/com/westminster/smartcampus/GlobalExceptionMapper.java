package com.westminster.smartcampus;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Catch-all ExceptionMapper for unexpected runtime errors.
 *
 * Mapped to Exception (not Throwable) so that more specific
 * mappers — RoomNotEmptyExceptionMapper, LinkedResourceNotFoundExceptionMapper,
 * SensorUnavailableExceptionMapper — always take priority for their
 * own exception types. JAX-RS picks the most specific registered
 * mapper for any thrown exception.
 *
 * Without this safety net, an uncaught NullPointerException would
 * leak a Java stack trace to the client — a serious information
 * disclosure risk. We log the full exception server-side and return
 * a sanitized 500 to the client.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Exception ex) {
        // Let JAX-RS's own exceptions through — their built-in mappers
        // already produce the correct status code.
        if (ex instanceof WebApplicationException wae) {
            return wae.getResponse();
        }

        // Log the real exception server-side for debugging.
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by GlobalExceptionMapper", ex);

        // Return a sanitized response — no stack trace, no internals.
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorMessage(
                        "An unexpected internal error occurred. "
                        + "Please contact the API administrator if the problem persists."))
                .build();
    }
}