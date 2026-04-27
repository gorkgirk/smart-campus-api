package com.westminster.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import java.io.IOException;
import java.net.URI;

/**
 * Main class — bootstraps the embedded Grizzly HTTP server and
 * deploys the Smart Campus JAX-RS application on it.
 */
public class Main {

    // Base URI the Grizzly HTTP server will listen on.
    // Combined with @ApplicationPath("/api/v1") on SmartCampusApplication,
    // all endpoints will be reachable under http://localhost:8080/api/v1/...
    public static final String BASE_URI = "http://localhost:8080/";

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources.
     * @return the running Grizzly HTTP server
     */
    public static HttpServer startServer() {
        // Instantiate our Application subclass (which extends ResourceConfig).
        // Passing this instance to Grizzly registers every @Path-annotated
        // class found by the package scan in SmartCampusApplication.
        final SmartCampusApplication app = new SmartCampusApplication();
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), app);
    }

    /**
     * Main entry point. Starts the server and blocks until the user
     * presses Enter (or Ctrl+C), at which point it shuts the server down.
     */
    public static void main(String[] args) throws IOException {
        final HttpServer server = startServer();
        System.out.println(String.format(
                "Smart Campus API started at %sapi/v1%nHit Enter to stop...",
                BASE_URI));
        System.in.read();
        server.stop();
    }
}