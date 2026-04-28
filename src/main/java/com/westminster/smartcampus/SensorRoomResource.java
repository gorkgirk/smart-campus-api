package com.westminster.smartcampus;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;
import java.net.URI;
import java.util.Collection;

/**
 * JAX-RS resource exposing CRUD-style endpoints for managing campus rooms.
 *
 * Combined with @ApplicationPath("/api/v1") on SmartCampusApplication,
 * the @Path("rooms") here means every method maps under /api/v1/rooms.
 *
 * The class is request-scoped by default (Jersey instantiates a new
 * SensorRoomResource per incoming request), so we keep state in the
 * RoomStore singleton rather than as instance fields.
 */
@Path("rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorRoomResource {

    // Shared singleton store — same data across all requests.
    private final RoomStore store = RoomStore.getInstance();

    /**
     * GET /api/v1/rooms
     * Returns the full list of rooms as a JSON array.
     */
    @GET
    public Collection<Room> listAllRooms() {
        return store.findAll();
    }

    /**
     * GET /api/v1/rooms/{roomId}
     * Returns the metadata for a single room.
     * Responds 404 Not Found if the ID doesn't exist.
     */
    @GET
    @Path("{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.findById(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage("Room with id '" + roomId + "' not found."))
                    .build();
        }
        return Response.ok(room).build();
    }

    /**
     * POST /api/v1/rooms
     * Creates a new room from the JSON request body.
     * Returns 201 Created with a Location header pointing at the new resource.
     *
     * @param room    the deserialized room from the request body
     * @param uriInfo injected by JAX-RS, used to build the Location URI
     */
    @POST
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        // Basic validation: room must have an ID and not collide with an existing one.
        if (room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMessage("Room id is required."))
                    .build();
        }
        if (store.exists(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorMessage("Room with id '" + room.getId() + "' already exists."))
                    .build();
        }

        store.save(room);

        // Build the URI for the newly-created resource — e.g. /api/v1/rooms/LIB-301
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(room.getId())
                .build();

        // 201 Created with Location header is the REST convention for resource creation.
        return Response.created(location).entity(room).build();
    }

    /**
     * DELETE /api/v1/rooms/{roomId}
     * Removes a room — but only if it has no sensors assigned, to prevent orphans.
     *
     * Business rule: a room with sensorIds attached cannot be deleted.
     * We throw a custom RoomNotEmptyException which is converted to
     * a 409 Conflict response by RoomNotEmptyExceptionMapper. Centralising
     * error formatting in the mapper keeps this resource focused on the
     * happy path and ensures every "room not empty" error has a uniform
     * shape across the API.
     */
    @DELETE
    @Path("{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.findById(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage("Room with id '" + roomId + "' not found."))
                    .build();
        }

        // Safety check: refuse to delete if sensors are still assigned.
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Room '" + roomId + "' cannot be deleted: it still has "
                    + room.getSensorIds().size() + " sensor(s) assigned.");
        }

        store.delete(roomId);
        // 204 No Content is conventional for successful DELETE with no body.
        return Response.noContent().build();
    }
}