package com.westminster.smartcampus;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root "Discovery" resource for the Smart Campus API.
 *
 * Mapped to GET /api/v1 (combining @ApplicationPath("/api/v1") from
 * SmartCampusApplication with the empty @Path here).
 *
 * Returns a JSON document describing the API: its version, who to
 * contact, and a map of the primary resource collections. This gives
 * clients a single entry point to discover what the API offers —
 * the starting point for HATEOAS-style navigation.
 */
@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getApiMetadata() {
        // LinkedHashMap preserves insertion order so the JSON fields
        // appear in a logical, predictable sequence in the response.
        Map<String, Object> metadata = new LinkedHashMap<>();

        metadata.put("apiName", "Smart Campus Sensor & Room Management API");
        metadata.put("version", "1.0");
        metadata.put("description",
                "RESTful service for managing rooms and IoT sensors across the university campus.");

        // Administrative contact details, grouped as a nested JSON object.
        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("name", "Smart Campus Backend Team");
        contact.put("email", "smartcampus-admin@westminster.ac.uk");
        contact.put("organisation", "University of Westminster — School of Computer Science");
        metadata.put("contact", contact);

        // Hypermedia links to the primary resource collections (HATEOAS).
        // Clients can follow these without knowing the URL structure in advance.
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("self", "/api/v1");
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        metadata.put("resources", resources);

        return metadata;
    }
}