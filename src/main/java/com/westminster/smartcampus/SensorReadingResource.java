package com.westminster.smartcampus;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * JAX-RS sub-resource handling /api/v1/sensors/{sensorId}/readings.
 *
 * This class is NOT annotated with @Path — instead, it is returned
 * by a sub-resource locator method on SensorResource. The parent
 * resource passes in the sensor context (the Sensor object) when
 * constructing this instance, so this class always knows which
 * sensor it operates on.
 *
 * Architectural benefit: SensorResource handles sensor-level
 * concerns (list, create, filter), while this class handles
 * reading-level concerns (history, append). Keeping them in
 * separate classes prevents a single "controller" from sprawling
 * as the API grows.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final Sensor sensor;
    private final SensorReadingStore readingStore = SensorReadingStore.getInstance();

    /**
     * Constructed by SensorResource's sub-resource locator with the
     * already-resolved parent Sensor. If the locator passes null
     * (sensor not found), this resource returns 404 from every method.
     */
    public SensorReadingResource(Sensor sensor) {
        this.sensor = sensor;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Returns the historical list of readings for the parent sensor.
     */
    @GET
    public Response listReadings() {
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage("Parent sensor not found."))
                    .build();
        }
        List<SensorReading> history = readingStore.findBySensor(sensor.getId());
        return Response.ok(history).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     * Appends a new reading to the parent sensor's history.
     *
     * Side effect (rubric requirement, Part 4.2): the parent sensor's
     * currentValue is updated to the new reading's value so the
     * sensor object always exposes the most recent measurement.
     *
     * Business rule (Part 5.3): if the sensor's status is
     * "MAINTENANCE", new readings are rejected. We throw a custom
     * SensorUnavailableException, which the dedicated mapper converts
     * to a 403 Forbidden response — keeping this method focused on
     * the happy path.
     */
    @POST
    public Response addReading(SensorReading reading, @Context UriInfo uriInfo) {
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage("Parent sensor not found."))
                    .build();
        }

        // Block readings on sensors under maintenance.
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensor.getId() + "' is under MAINTENANCE; "
                    + "readings are not accepted.");
        }

        // Auto-generate id and timestamp if the client didn't supply them.
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0L) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Persist the reading.
        readingStore.append(sensor.getId(), reading);

        // Side effect: refresh the parent sensor's currentValue so
        // GET /sensors/{id} always reflects the latest measurement.
        sensor.setCurrentValue(reading.getValue());

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(reading.getId())
                .build();
        return Response.created(location).entity(reading).build();
    }
}