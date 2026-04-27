package com.westminster.smartcampus;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * In-memory storage for Sensor objects, mirroring RoomStore's design.
 *
 * Uses a ConcurrentHashMap so multiple HTTP threads can read and
 * write concurrently without explicit synchronization. The singleton
 * pattern guarantees every JAX-RS resource sees the same dataset.
 */
public class SensorStore {

    private static final SensorStore INSTANCE = new SensorStore();

    private final ConcurrentMap<String, Sensor> sensors = new ConcurrentHashMap<>();

    private SensorStore() {
        // Seed with a sample sensor so testing/demoing is easier.
        // Note: this sensor's roomId matches one of the seed rooms in RoomStore.
        Sensor temp = new Sensor("TEMP-001", "Temperature", "ACTIVE", 21.5, "LIB-301");
        sensors.put(temp.getId(), temp);
    }

    public static SensorStore getInstance() {
        return INSTANCE;
    }

    /** All sensors. */
    public Collection<Sensor> findAll() {
        return sensors.values();
    }

    /**
     * Filter sensors by type (case-insensitive substring match wouldn't
     * be appropriate — the brief implies exact-type filtering, e.g.
     * ?type=CO2 returns only CO2 sensors).
     */
    public List<Sensor> findByType(String type) {
        return sensors.values().stream()
                .filter(s -> s.getType() != null && s.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    public Sensor findById(String id) {
        return sensors.get(id);
    }

    public boolean exists(String id) {
        return sensors.containsKey(id);
    }

    public void save(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
    }

    public Sensor delete(String id) {
        return sensors.remove(id);
    }
}