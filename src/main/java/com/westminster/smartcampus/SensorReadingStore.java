package com.westminster.smartcampus;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory storage for sensor readings.
 *
 * Keyed by sensorId — each sensor has an independently-growing list
 * of readings. We use CopyOnWriteArrayList for the inner lists so
 * that concurrent reads (e.g. simultaneous GET history requests)
 * never see a partially-mutated list while a POST appends a new
 * reading. CoW is well-suited here because reads are expected to
 * outnumber writes per sensor.
 */
public class SensorReadingStore {

    private static final SensorReadingStore INSTANCE = new SensorReadingStore();

    // Outer map: sensorId -> readings list. ConcurrentHashMap handles
    // adding new sensors safely; CopyOnWriteArrayList handles the
    // per-sensor reads/writes safely.
    private final ConcurrentMap<String, List<SensorReading>> readingsBySensor =
            new ConcurrentHashMap<>();

    private SensorReadingStore() {
    }

    public static SensorReadingStore getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the (possibly empty) list of readings for the given sensor.
     * The returned list is unmodifiable to prevent callers from
     * accidentally mutating the stored history.
     */
    public List<SensorReading> findBySensor(String sensorId) {
        List<SensorReading> readings = readingsBySensor.get(sensorId);
        if (readings == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(readings);
    }

    /**
     * Appends a new reading to the given sensor's history.
     * Lazily creates the inner list on first use.
     */
    public void append(String sensorId, SensorReading reading) {
        readingsBySensor
                .computeIfAbsent(sensorId, key -> new CopyOnWriteArrayList<>())
                .add(reading);
    }
}