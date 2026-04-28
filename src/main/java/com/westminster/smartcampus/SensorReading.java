package com.westminster.smartcampus;

/**
 * Domain model representing a single timestamped measurement
 * recorded by a sensor.
 *
 * Each reading has its own UUID-style id, the epoch-millisecond
 * timestamp at which it was captured, and the value itself.
 *
 * Readings are not stored on the Sensor object — they live in
 * SensorReadingStore keyed by the parent sensor's ID, so that
 * the history can grow without bloating individual Sensor instances.
 */
public class SensorReading {

    private String id;       // Unique reading event ID (UUID recommended)
    private long timestamp;  // Epoch milliseconds when the reading was captured
    private double value;    // The measurement value

    public SensorReading() {
    }

    public SensorReading(String id, long timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}