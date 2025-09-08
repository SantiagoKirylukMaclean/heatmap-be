# Heatmap Backend

Spring Boot backend that exposes aggregated heatmap data over US regions and H3 cells. It ingests/reads price and sales information from PostgreSQL, aggregates by period and spatial granularity, and serves results via REST endpoints with HTTP caching (ETag). Swagger/OpenAPI documentation is included.

## Tech Stack
- Java 17, Spring Boot 3.5
- Spring Web, Data JPA/JDBC, Actuator, Cache (Redis)
- PostgreSQL + Flyway migrations
- Redis for caching
- OpenAPI docs via springdoc
- Uber H3 for hexagonal grid aggregation
- Gradle build

## Project Structure
- `com.puetsnao.heatmap` domain/application/infrastructure packages (hexagonal layering)
  - api: REST controllers
  - application: use cases/services
  - domain: core types and value objects
  - infrastructure: persistence, caching, scheduling, observability

## Prerequisites
- Java 17+
- Docker and Docker Compose (to run PostgreSQL and Redis)

## Quick Start
1) Start infrastructure services

```
docker compose up -d
```
This launches:
- PostgreSQL 16 at localhost:5432 (db: heatmap / user: heatmap / pass: heatmap)
- Redis 7 at localhost:6379

2) Run the application (dev profile is the default)

```
./gradlew bootRun
```
The app starts on http://localhost:8080

3) Open API docs
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Configuration
Primary configuration files:
- `src/main/resources/application.yml` (defaults):
  - `spring.profiles.default: dev`
  - `springdoc.swagger-ui.path: /swagger-ui.html`
  - Actuator endpoints enabled (health, info, metrics, scheduledtasks)
  - `heatmap.cache.ttl-seconds` (default 300)
  - `heatmap.summary-refresh.enabled` (default false)
- `src/main/resources/application-dev.yml` (dev overrides):
  - PostgreSQL connection to localhost
  - Redis enabled as cache provider
  - `heatmap.summary-refresh.enabled: true` for background summary refresh in dev

Caching:
- Redis is used as cache provider in dev
- TTL can be configured via `heatmap.cache.ttl-seconds`

Migrations:
- Flyway manages schema under `src/main/resources/db/migration`

Dev dataset seeding:
- In the `dev` profile, `DevDatasetLoader` can generate data when price/sales are empty.
  - Default mode: multi-day light dataset across seeded stations
  - Heavy NJ mode (opt-in): set in application-dev.yml
    - dev.seed.enabled: true
    - dev.seed.nj-stations-count: 8000 (configurable)
    - dev.seed.day: 2025-09-08
    - heatmap.summary-refresh.target-date: 2025-09-08 and window-days: 1
    - Generates only New Jersey stations and hourly price/sales for that day to approximate ~2M H3 summary rows (H2..H15)

## REST API
Base URL: `http://localhost:8080`

Health
- GET `/api/health`
  - 200 OK: `{ "status": "UP", "commitSha": "..." }` (commitSha present when available)

Heatmap by state
- GET `/api/heatmap`
  - Query params:
    - `metric` (required): `price` | `volume`
    - `period` (optional, default `last30d`): `last30d`
  - ETag: supports `If-None-Match` and responds `304 Not Modified` when unchanged
  - 200 OK: JSON array of heat points: `[{ "state": "TX", "lat": 29.76, "lon": -95.36, "value": 2.15 }]`

Heatmap by H3 cell
- GET `/api/heatmap/h3`
  - Query params:
    - `metric` (required): `price` | `volume`
    - `resolution` (required): integer H3 resolution (2 to 15)
    - `bucket` (optional, default `day`): `day` | `hour`
    - `at` (optional): `YYYY-MM-DD` when bucket=day, or `YYYY-MM-DDTHH:00` when bucket=hour
  - ETag: supports `If-None-Match` and responds `304 Not Modified` when unchanged
  - 200 OK: JSON array of H3 cell points: `[{ "cell": "85283473fffffff", "resolution": 7, "value": 2.15 }]`

### cURL examples
Health
```
curl -s http://localhost:8080/api/health | jq
```

State heatmap
```
# First call (returns data + ETag)
curl -i "http://localhost:8080/api/heatmap?metric=price&period=last30d"

# Subsequent call with ETag to leverage 304
curl -i -H "If-None-Match: <etag-from-previous>" \
  "http://localhost:8080/api/heatmap?metric=price&period=last30d"
```

H3 heatmap
```
# Day bucket at a given date
curl -s "http://localhost:8080/api/heatmap/h3?metric=price&resolution=7&bucket=day&at=2025-09-01" | jq

# Hour bucket for a given hour
curl -s "http://localhost:8080/api/heatmap/h3?metric=volume&resolution=7&bucket=hour&at=2025-09-01T10:00" | jq
```

## Running Tests
```
./gradlew test
```

## Building
```
./gradlew clean build
```
The packaged JAR will be under `build/libs/`.

## Actuator
- Health: `/actuator/health`
- Info: `/actuator/info`
- Metrics: `/actuator/metrics`
- Scheduled tasks: `/actuator/scheduledtasks`

## Troubleshooting
- PostgreSQL connection refused: ensure `docker compose up -d` started services and port 5432 is free.
- Flyway validation errors after schema changes: stop the app and `docker compose down -v` to recreate DB volume for a clean dev database.
- Redis not reachable: ensure Redis container is running at port 6379.

## License
TBD. Specify the project license here.
