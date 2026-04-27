package com.westminster.smartcampus;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain model representing a physical room on the Smart Campus.
 *
 * A Room is one of three core resources in the API. Each room has a
 * unique ID (e.g. "LIB-301"), a human-readable name, a capacity for
 * occupancy safety, and a list of sensor IDs deployed inside it.
 *
 * The class follows the JavaBean convention (no-arg constructor,
 * private fields, public getters/setters) so Jackson can serialize
 * it to/from JSON automatically.
 */
public class Room {

    private String id;                              // Unique ID, e.g. "LIB-301"
    private String name;                            // Human-readable name
    private int capacity;                           // Maximum occupancy
    private List<String> sensorIds = new ArrayList<>(); // IDs of sensors in this room

    // No-arg constructor — required by Jackson for JSON deserialization.
    public Room() {
    }

    // Convenience constructor for creating rooms in code (e.g. seed data).
    public Room(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }

    // --- Getters & setters (required for Jackson and JAX-RS) ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<String> getSensorIds() {
        return sensorIds;
    }

    public void setSensorIds(List<String> sensorIds) {
        this.sensorIds = sensorIds;
    }
}