package com.westminster.smartcampus;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;

/**
 * JAX-RS resource exposing endpoints for managing sensors.
 *
 * Combined with @ApplicationPath("/api/v1"), every method here is
 * mounted under /api/v1/sensors.
 *
 * The class is request-scoped (a new instance per HTTP request),
 * so all shared state is held in the SensorStore singleton — and
 * cross-resource references (sensor -> room) go through RoomStore.
 */
@Path("sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final SensorStore sensorStore = SensorStore.getInstance();
    private final RoomStore roomStore = RoomStore.getInstance();

    /**
     * GET /api/v1/sensors
     * GET /api/v1/sensors?type=Temperature
     *
     * Returns the full list of sensors, optionally filtered by type
     * via the @QueryParam.
     *
     * Using a query parameter (rather than a path segment like
     * /sensors/type/Temperature) is conventional for filtering
     * collections: queries can be combined, omitted, or extended
     * without changing the URL hierarchy.
     */
    @GET
    public Collection<Sensor> listSensors(@QueryParam("type") String type) {
        if (type == null || type.isBlank()) {
            return sensorStore.findAll();
        }
        return sensorStore.findByType(type);
    }

    /**
     * GET /api/v1/sensors/{sensorId}
     * Fetch a single sensor by ID. 404 if it doesn't exist.
     */
    @GET
    @Path("{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = sensorStore.findById(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage("Sensor with id '" + sensorId + "' not found."))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    /**
     * POST /api/v1/sensors
     *
     * Registers a new sensor. The roomId in the body MUST refer to
     * an existing room — otherwise we throw LinkedResourceNotFoundException,
     * which the dedicated mapper converts to HTTP 422 Unprocessable Entity.
     *
     * On success: 201 Created + Location header pointing at the
     * newly-registered sensor.
     *
     * Side effect: the sensor's ID is also pushed onto the parent
     * room's sensorIds list, keeping the bidirectional link in sync.
     */
    @POST
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        // Basic field validation
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMessage("Sensor id is required."))
                    .build();
        }
        if (sensorStore.exists(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorMessage("Sensor with id '" + sensor.getId() + "' already exists."))
                    .build();
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            throw new LinkedResourceNotFoundException(
                    "roomId is required to register a sensor.");
        }

        // Foreign-key validation: the referenced room must exist.
        // 422 Unprocessable Entity is more semantically accurate than 404
        // here because the JSON itself is well-formed — only the *referenced*
        // resource is missing. The exception mapper handles the response shape.
        Room parentRoom = roomStore.findById(sensor.getRoomId());
        if (parentRoom == null) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor: room '" + sensor.getRoomId()
                    + "' does not exist.");
        }

        // Save the sensor and update the parent room's sensorIds list.
        sensorStore.save(sensor);
        if (!parentRoom.getSensorIds().contains(sensor.getId())) {
            parentRoom.getSensorIds().add(sensor.getId());
        }

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(sensor.getId())
                .build();
        return Response.created(location).entity(sensor).build();
    }

    /**
     * Sub-resource locator for /api/v1/sensors/{sensorId}/readings.
     *
     * Note: this method is annotated with @Path but NOT with @GET,
     * @POST, etc. That's the JAX-RS "locator" pattern: rather than
     * handling the request itself, the method returns a new resource
     * instance (SensorReadingResource) which then handles the
     * remainder of the URL with its own annotated methods.
     *
     * This keeps SensorResource focused on sensor-level concerns
     * and pushes per-sensor reading logic into a dedicated class.
     */
    @Path("{sensorId}/readings")
    public SensorReadingResource readings(@PathParam("sensorId") String sensorId) {
        Sensor parent = sensorStore.findById(sensorId);
        // Even if parent is null we still return a SensorReadingResource;
        // it will produce a 404 itself. Returning null here would cause
        // JAX-RS to throw a generic 500.
        return new SensorReadingResource(parent);
    }
}