package com.westminster.smartcampus;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * JAX-RS Application subclass for the Smart Campus API.
 *
 * The @ApplicationPath annotation sets the versioned base path for all
 * resources in this API. Every @Path defined on a resource class will be
 * relative to "/api/v1", giving us a clean versioned entry point.
 *
 * Extending ResourceConfig (which itself extends jakarta.ws.rs.core.Application)
 * lets us register resources by package-scanning, which is the standard
 * Jersey idiom for modular REST APIs.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {

    public SmartCampusApplication() {
        // Scan the base package (and any sub-packages) for classes annotated
        // with @Path, @Provider, etc. Any resource or exception mapper we
        // add under com.westminster.smartcampus will be auto-registered.
        packages("com.westminster.smartcampus");
    }
}