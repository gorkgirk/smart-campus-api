package com.westminster.smartcampus;

/**
 * Domain model representing a single IoT sensor on the Smart Campus.
 *
 * Each sensor belongs to one room (via the roomId foreign-key),
 * has a category (type), an operational status, and a most-recent
 * measurement (currentValue) which is updated whenever a new
 * reading is posted under it.
 *
 * Like Room, this is a JavaBean (no-arg constructor + getters/setters)
 * so Jackson can marshal it to/from JSON.
 */
public class Sensor {

    private String id;            // Unique sensor ID, e.g. "TEMP-001"
    private String type;          // Category, e.g. "Temperature", "CO2", "Occupancy"
    private String status;        // "ACTIVE", "MAINTENANCE", or "OFFLINE"
    private double currentValue;  // Most recent reading value
    private String roomId;        // Foreign key — the room this sensor lives in

    public Sensor() {
    }

    public Sensor(String id, String type, String status, double currentValue, String roomId) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.currentValue = currentValue;
        this.roomId = roomId;
    }

    // --- Getters & setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}