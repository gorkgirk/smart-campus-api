package com.westminster.smartcampus;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory storage for Room objects.
 *
 * Implemented as a singleton (eager initialization via a static final
 * INSTANCE field) so every JAX-RS resource sees the same data, even
 * though resource classes themselves are instantiated per request.
 *
 * ConcurrentHashMap provides thread-safe reads and writes without
 * forcing us to wrap every access in synchronized blocks. This is
 * essential because Grizzly handles many concurrent HTTP requests on
 * separate threads — a plain HashMap would corrupt under load.
 */
public class RoomStore {

    // Eagerly-initialized singleton instance.
    private static final RoomStore INSTANCE = new RoomStore();

    // The underlying thread-safe map: key = room ID, value = Room object.
    private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

    // Private constructor prevents external instantiation.
    // Seeds the store with a few sample rooms for easier testing/demo.
    private RoomStore() {
        Room library = new Room("LIB-301", "Library Quiet Study", 40);
        Room lab = new Room("LAB-101", "Computer Science Lab 1", 30);
        rooms.put(library.getId(), library);
        rooms.put(lab.getId(), lab);
    }

    public static RoomStore getInstance() {
        return INSTANCE;
    }

    /** Returns all rooms currently in the store. */
    public Collection<Room> findAll() {
        return rooms.values();
    }

    /** Returns the room with the given ID, or null if not found. */
    public Room findById(String id) {
        return rooms.get(id);
    }

    /** Returns true if a room with this ID exists. */
    public boolean exists(String id) {
        return rooms.containsKey(id);
    }

    /** Saves (creates or overwrites) a room. */
    public void save(Room room) {
        rooms.put(room.getId(), room);
    }

    /** Removes the room with the given ID. Returns the removed room, or null if it didn't exist. */
    public Room delete(String id) {
        return rooms.remove(id);
    }
}