package com.westminster.smartcampus;

/**
 * Thrown when a request references a foreign-key resource that
 * doesn't exist (e.g. POSTing a Sensor with a roomId that's not in
 * RoomStore). Mapped to HTTP 422 Unprocessable Entity.
 *
 * 422 is more semantically accurate than 404 here: the request URL
 * itself is valid (POST /sensors), and the JSON syntax is fine —
 * only a *referenced* resource inside the payload is missing. 404
 * would imply the endpoint itself doesn't exist.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}