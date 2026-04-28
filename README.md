# Smart Campus Sensor & Room Management API

5COSC022W — Client-Server Architectures Coursework (2025/26)
University of Westminster — School of Computer Science and Engineering

A RESTful web service built with **JAX-RS (Jersey 3.1.9)** for managing rooms and IoT sensors across the university campus. The API exposes resources for `Room`, `Sensor`, and per-sensor `SensorReading` history, with full exception mapping and request/response logging.

## Tech stack

- Java 17 (compiled with `<release>17</release>`)
- JAX-RS / Jersey 3.1.9 (Jakarta namespace)
- Embedded Grizzly HTTP server
- Jackson for JSON binding (via `jersey-media-json-jackson`)
- Maven for build and dependency management
- In-memory storage only — `ConcurrentHashMap` and `CopyOnWriteArrayList` (no database, per coursework constraints)

## Project structure
src/main/java/com/westminster/smartcampus/
├── Main.java                                    # Bootstraps Grizzly + the Application
├── SmartCampusApplication.java                  # @ApplicationPath("/api/v1") entry point
├── DiscoveryResource.java                       # GET /api/v1 metadata endpoint
├── Room.java, Sensor.java, SensorReading.java   # POJO domain models
├── RoomStore.java, SensorStore.java,            # Thread-safe in-memory singletons
│   SensorReadingStore.java
├── SensorRoomResource.java                      # /rooms CRUD
├── SensorResource.java                          # /sensors CRUD + filtering + locator
├── SensorReadingResource.java                   # /sensors/{id}/readings sub-resource
├── ErrorMessage.java                            # Uniform JSON error wrapper
├── RoomNotEmptyException.java + Mapper          # 409 Conflict
├── LinkedResourceNotFoundException + Mapper     # 422 Unprocessable Entity
├── SensorUnavailableException + Mapper          # 403 Forbidden
├── GlobalExceptionMapper.java                   # 500 catch-all (sanitised)
└── LoggingFilter.java                           # Request/response logging
## Build and run

Prerequisites: Java 17+ and Maven 3.9+ on the `PATH`.

```bash
# Clone
git clone https://github.com/gorkgirk/smart-campus-api.git
cd smart-campus-api

# Build
mvn clean compile

# Run (starts Grizzly on http://localhost:8080)
mvn exec:java
```

The API base URL is `http://localhost:8080/api/v1`. Press `Enter` in the terminal to stop the server.

## Sample curl commands

```bash
# 1. Discovery — get API metadata
curl http://localhost:8080/api/v1

# 2. List all rooms
curl http://localhost:8080/api/v1/rooms

# 3. Create a new room (201 Created + Location header)
curl -i -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"ENG-205","name":"Engineering Workshop","capacity":25}'

# 4. Register a new sensor (validates that roomId exists)
curl -i -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","currentValue":412.3,"roomId":"LAB-101"}'

# 5. Filter sensors by type via @QueryParam
curl "http://localhost:8080/api/v1/sensors?type=CO2"

# 6. Append a new reading to a sensor (auto-updates parent's currentValue)
curl -i -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.7}'

# 7. Try to delete a room that still has sensors → 409 Conflict
curl -i -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

---

# Conceptual Report — Answers to Coursework Questions

## Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS resource classes are **request-scoped**: the runtime instantiates a fresh instance of `SensorRoomResource`, `SensorResource`, etc. for every incoming HTTP request. This means instance fields on a resource class cannot reliably hold state between requests — anything written to `this.someField` vanishes when the request finishes and the instance is garbage-collected.

This has direct implications for shared in-memory storage. A naive design that put a `HashMap<String, Room>` directly on the resource class would lose data on every request because the map would be re-initialised each time. To work around this, the project uses **eagerly-initialised singleton stores** (`RoomStore.INSTANCE`, `SensorStore.INSTANCE`, `SensorReadingStore.INSTANCE`) that all resource instances share.

Because the embedded Grizzly server services concurrent HTTP requests on a thread pool, those shared maps must also be safe for concurrent reads and writes. A plain `HashMap` would corrupt under load — entries can be lost, infinite loops can form during resize, and iterators can throw `ConcurrentModificationException`. The implementation therefore uses `ConcurrentHashMap` for the room and sensor stores, and `CopyOnWriteArrayList` for per-sensor reading lists in `SensorReadingStore`. These collections provide thread-safe behaviour without forcing every access through `synchronized` blocks, which would serialise all requests through a single lock and destroy throughput. CoW is well-suited to the readings list because reads (history queries) are expected to outnumber writes (new readings) per sensor.

## Part 1.2 — HATEOAS

HATEOAS (Hypermedia as the Engine of Application State) is the principle that responses include links to related actions and resources, so that clients can navigate the API by following links rather than hard-coding URL templates. The Discovery endpoint at `GET /api/v1` is the entry point for this navigation: it returns a `resources` map listing every primary collection (`/rooms`, `/sensors`) along with a `self` link.

Compared to static documentation, HATEOAS offers three concrete benefits to client developers. First, it **decouples clients from URL structure** — if `/api/v1/rooms` were renamed to `/api/v1/spaces` in a future version, a HATEOAS-aware client that reads the resource map at runtime would adapt without code changes, whereas a client with hard-coded URLs would break. Second, it makes APIs **self-describing**: a developer (or an automated tool like Swagger UI) can begin at the root and discover every endpoint without external documentation. Third, it enables **client logic to follow the workflow encoded by the server** — the server can include or omit links based on resource state (for example, omitting a "delete" link for a room that has active sensors), which keeps business rules in one place rather than duplicating them across every client.

## Part 2.1 — Returning IDs vs Full Objects

Returning only IDs from a list endpoint reduces payload size dramatically — a list of 10,000 rooms might be 200 KB of IDs versus several MB of full objects with names, capacities, and embedded sensor lists. This matters for mobile clients on slow connections and for high-frequency polling.

However, ID-only responses force the client to issue **N additional GET requests** to fetch each room's details (the classic N+1 problem), which destroys overall latency and increases server load far beyond what the bandwidth saving justifies. Returning full objects, as this implementation does, accepts the larger payload in exchange for a single round trip and simpler client code. For larger datasets the appropriate compromise is **pagination** combined with field-selection query parameters (e.g. `?fields=id,name`), giving clients control over the trade-off without forcing one mode globally.

## Part 2.2 — Idempotency of DELETE

`DELETE` is **idempotent by HTTP specification** — issuing the same delete request multiple times must have the same effect as issuing it once. In this implementation:

- The first `DELETE /api/v1/rooms/ENG-205` finds the room, runs the safety check, removes it from `RoomStore`, and returns `204 No Content`.
- The second identical request finds no such room and returns `404 Not Found`.

Although the status codes differ across calls, the **server state after each call is identical** — the room is absent. That's the correct definition of idempotency: it concerns final state, not response equality. Some APIs choose to return `204` on the second call too (treating "delete a non-existent thing" as a no-op success), but `404` is more honest about what happened and lets the client distinguish between "this delete actually removed something" and "this thing was already gone".

## Part 3.1 — `@Consumes(MediaType.APPLICATION_JSON)` and Content-Type mismatches

When a resource method is annotated with `@Consumes(MediaType.APPLICATION_JSON)`, JAX-RS only routes a request to that method if the request's `Content-Type` header is `application/json` (or is omitted, since some clients default). If a client sends `text/plain` or `application/xml` to a method that consumes only JSON, the JAX-RS runtime intercepts the request before it reaches the resource method and returns **HTTP 415 Unsupported Media Type** automatically. The client never reaches the deserialisation step.

This is implemented in Jersey through its `MessageBodyReader` provider chain: when no reader is registered for the requested content type / target Java type combination, the runtime aborts the dispatch. From a developer's perspective this is hugely beneficial — content-negotiation logic lives in declarative annotations rather than imperative `if (request.getContentType().equals(...))` checks scattered through resource code.

## Part 3.2 — `@QueryParam` vs path-based filtering

Path-based filtering (`/sensors/type/CO2`) treats the filter as **part of the resource hierarchy**, which it isn't. Conceptually, "all CO2 sensors" is not a separate resource — it's a *view* of the existing `/sensors` collection. Modelling it as a path implies a permanent resource that doesn't really exist, and forces an awkward URL design when more filters are needed (`/sensors/type/CO2/status/ACTIVE`?).

`@QueryParam` filtering (`/sensors?type=CO2`) is the standard solution because it expresses what's actually happening: a query against the existing collection. Multiple filters compose naturally with `&` (`?type=CO2&status=ACTIVE`), filters can be omitted to mean "all" (an empty query parameter naturally falls back to the unfiltered list), and the URL space stays flat and predictable. It also matches the HTTP convention used by virtually every public REST API for collection filtering, search, sorting, and pagination.

## Part 4.1 — Sub-Resource Locator pattern

The sub-resource locator pattern (used by `SensorResource.readings(...)` returning a `SensorReadingResource`) lets a parent resource delegate further URL handling to a dedicated class. The benefits become obvious as an API grows.

Without the locator, every nested path would have to be defined as another method on the parent resource — `getReadings`, `addReading`, `getReadingById`, `deleteReading`, etc. — until the parent class becomes a thousand-line "controller" handling unrelated concerns. With the locator, `SensorResource` only handles sensor-level concerns (list, create, filter), and `SensorReadingResource` handles reading-level concerns (history, append). Each class stays small and testable.

The locator also lets the parent **inject context** into the sub-resource — in this implementation, the parent looks up the `Sensor` once and passes it into the `SensorReadingResource` constructor, so sub-resource methods can assume the sensor is already resolved (or null with a 404). This eliminates duplicated lookup code that would otherwise appear in every sub-method.

## Part 5.2 — 422 vs 404 for missing references

`404 Not Found` is for situations where the **request URL itself targets nothing** — `GET /sensors/DOES-NOT-EXIST` is a clear 404 because the sensor at that URL doesn't exist. `422 Unprocessable Entity` (RFC 4918) is for situations where the **URL is valid and the request payload is well-formed JSON, but a referenced resource inside the payload is missing**. The semantic distinction is important: 404 says "the thing you asked for isn't here", whereas 422 says "I understood you, but I can't fulfil this because something you referenced is missing."

When a client `POST /api/v1/sensors` with a body containing `"roomId": "DOES-NOT-EXIST"`, the URL `/api/v1/sensors` is valid (the endpoint exists), the JSON parses fine, and the request shape is correct. Returning 404 here would falsely imply the endpoint itself is missing, confusing automated clients. 422 correctly communicates "your request is syntactically fine; it's the linked data that's the problem", which is exactly what `LinkedResourceNotFoundException` reports.

## Part 5.4 — Risks of exposing Java stack traces

Exposing raw stack traces to API consumers leaks substantial information that an attacker can weaponise. From a single trace they can typically identify:

- The **exact framework versions** in use (e.g. Jersey 3.1.9, Jackson 2.17.2) — which lets the attacker look up known CVEs against those exact versions and target unpatched vulnerabilities.
- **Internal package and class structure** (`com.westminster.smartcampus.SensorRoomResource`), revealing the codebase's organisation and naming conventions, useful for guessing other endpoints or for social engineering.
- **Server-side file paths and line numbers**, which can hint at the deployment environment.
- The **type of the underlying exception** (e.g. `JsonMappingException`, `SQLException`), which can confirm the presence and behaviour of specific subsystems.
- **Database schema details**, query fragments, or ORM internals if the trace originated from data access code.

The `GlobalExceptionMapper` mitigates this by logging the full exception server-side (so developers can debug) but returning a deliberately bland `"An unexpected internal error occurred"` message to the client. This is the same defence-in-depth principle behind not echoing SQL errors directly to web users.

## Part 5.5 — Why filters beat manual logging

JAX-RS filters are the right place for **cross-cutting concerns** — behaviours that apply uniformly to every request rather than specific to one endpoint. Logging is the textbook example. Inserting `Logger.info(...)` calls inside every resource method has three problems:

First, it **violates DRY** — the same boilerplate appears in dozens of methods, and adding a new logging field (e.g. response time) requires editing every one. Second, it's **easy to forget** — a developer adding a new endpoint can ship it without logging, and the gap is invisible until something breaks in production. Third, it **conflates concerns** — resource methods should describe business logic (create a sensor, append a reading), not mix in logging or auditing or metrics.

`LoggingFilter` solves all three by registering once, applying automatically to every request and response, and being completely separate from resource code. The same pattern works for authentication, request-ID injection, response compression, caching headers, and metrics. This is the JAX-RS analogue of Spring's `HandlerInterceptor` or Express's middleware — a well-trodden architectural pattern for keeping core code clean while still implementing cross-cutting requirements.

---

## Notes on the Jakarta namespace

The coursework brief shows imports like `javax.ws.rs.core.Application`. This implementation uses the equivalent `jakarta.ws.rs.core.Application` because Jersey 3.x is built on the Jakarta EE 9+ namespace (which migrated from `javax.*` to `jakarta.*` in 2020). All annotations and APIs behave identically — only the import prefix differs.
