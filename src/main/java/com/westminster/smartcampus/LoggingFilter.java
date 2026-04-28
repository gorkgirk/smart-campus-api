package com.westminster.smartcampus;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Cross-cutting request/response logging filter.
 *
 * Implements both ContainerRequestFilter (runs before any resource
 * method) and ContainerResponseFilter (runs after the resource
 * method returns, before the response is sent). This single class
 * therefore captures every HTTP exchange end-to-end with no need
 * to insert Logger.info() calls into individual resource methods.
 *
 * @Provider lets the package scan in SmartCampusApplication find
 * and register this filter automatically.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /** Logs every incoming HTTP request: method + URI. */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format("--> %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()));
    }

    /** Logs every outgoing HTTP response: final status code. */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format("<-- %s %s -> %d",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus()));
    }
}