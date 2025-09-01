# OPIS HeatMap MVP — Backend (Spring Boot + SQL + Redis)

## 🎯 Goal
Backend in **Spring Boot 3 (Java 21)** to expose an API powering a dashboard with **heat maps** of US fuel prices and sales.

The MVP is a **monolithic app**, but designed with clear boundaries to evolve into microservices in the future.

---

## ⚙️ Architecture

- **Spring Boot 3 + Gradle**
- **Relational DB**: PostgreSQL (MVP) with MSSQL support (production).
- **ORM / JPA** with **Flyway** for migrations.
- **Redis** for caching aggregated results.
- **Springdoc OpenAPI** for API documentation.
- **Spring Batch or @Scheduled** jobs for pre-aggregation.

Flow:
```
Client (Dashboard) --> HeatMap API --> HeatMap Service --> Redis Cache
                                              │
                                      Aggregated Views / Batch Jobs
                                              │
                                      Station / Product / Price / Sales (DB)
```

---


## 🗄️ Data Model

**Station**
- id (PK), code, name, state, latitude, longitude  

**Product**
- id (PK), code, name, family  

**Price** (price history)
- id (PK), station_id (FK), product_id (FK), ts, price  

**Sales** (aggregated sales)
- id (PK), station_id (FK), product_id (FK), ts, volume, revenue?, tx_count?  

🔗 Relations:
- Station (1) ──< Price, Sales  
- Product (1) ──< Price, Sales  

---

## 🧩 HeatMap Component

A **vertical slice** within the monolith containing:
- **Repository Layer** → access to Station, Product, Price, Sales.  
- **Service Layer** → aggregation logic (avg price, total volume, etc).  
- **Cache Layer (Redis)** → precomputed results (`metric:period`).  
- **API Layer (REST)** →  
  - `GET /api/heatmap?metric=price|volume&period=last30d`

Example response:
```json
[
  { "state": "TX", "lat": 29.76, "lon": -95.36, "value": 2.15 },
  { "state": "IL", "lat": 41.87, "lon": -87.62, "value": 2.09 }
]
```

---

## 📊 Scaling Strategy (2B+ rows)

- **Pre-aggregation**: materialized views or summary tables (by day/state/product).  
- **Batch/Scheduler**: refresh offline (daily/hourly).  
- **Indexing & partitioning**: `(station_id, product_id, ts)` + monthly/yearly partitions.  
- **Redis cache**: dashboard always queries precomputed and cached results.

---

## ❓ Interview Preparation

### 1. Monolith vs. Microservices
- Experience in both (Java EE monoliths and Spring Boot microservices).  
- MVP preference: **modular monolith** (faster delivery, less overhead).  
- Clear boundaries designed for future migration.

**Key phrase:**  
> “I like to start with a modular monolith, deliver value quickly, but design boundaries so that they can evolve into microservices when needed.”

---

### 2. Handling 2B rows
- Previous experience with millions of records/events.  
- Strategy: pre-aggregations in DB + batch jobs + Redis cache.  
- Frontend **never** queries raw tables.

**Key phrase:**  
> “With billions of rows, I would never let the frontend query raw tables. I’d precompute aggregates, store them in views, and expose them through cached APIs.”

---

### 3. HeatMap Component
- Contains repositories, aggregation logic, cache, and API contract.  
- Supported by frameworks: **Spring Batch**, **Redis**, **Flyway**, **OpenAPI**.  
- Heavy lifting done in DB/batch, lightweight runtime API.

**Key phrase:**  
> “The HeatMap component is a vertical slice: aggregation logic, caching, and API. Heavy lifting happens offline via batch jobs and materialized views.”

---

## 🚀 MVP Roadmap (for AI agents)

1. **Bootstrap**: Spring Boot app with Gradle.  
2. **DB migrations**: Flyway with Station, Product, Price, Sales tables.  
3. **Seed Data**: ~50k synthetic sales for local tests.  
4. **API**: implement `/api/heatmap`.  
5. **Cache**: integrate Redis.  
6. **Aggregation**: start with in-memory → migrate to SQL views.  
7. **Batch**: scheduled job to refresh aggregates.  
8. **Docs & Monitoring**: Swagger + Actuator.

---

## 🧭 Step-by-step Action Plan
- See [PLAN_accion_es.md](./PLAN_accion_es.md) for commit-by-commit steps and acceptance criteria (Spanish).

---

## ✅ MVP Success Criteria
- App runs locally with Docker (Postgres + Redis).  
- `/api/heatmap` responds in < 1s (thanks to cache).  
- DB ready to scale to 2B rows with materialized views + batch.  


---

## 🧪 Local development with Docker Compose

Prerequisites:
- Docker and Docker Compose
- JDK 21+
- Gradle wrapper (included)

Steps:
1. Start infrastructure (Postgres + Redis):
   - docker compose up -d
2. Run the application (dev profile is default):
   - ./gradlew bootRun
3. Verify it’s running:
   - curl http://localhost:8080/api/health
   - curl "http://localhost:8080/api/heatmap?metric=price&period=last30d"
4. Stop infrastructure when finished:
   - docker compose down -v

Notes:
- Dev datasource: jdbc:postgresql://localhost:5432/heatmap (user/password: heatmap)
- Redis: localhost:6379
- Flyway migrations run on startup in dev profile.


---

## 📚 API Documentation

- Swagger UI: http://localhost:8080/swagger-ui.html (or /swagger-ui/index.html)
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Endpoints:
- GET /api/health
  - Response: { "status": "UP", "commitSha": "<optional>" }
- GET /api/heatmap?metric=price|volume&period=last30d
  - Response: array of HeatPoint { state, lat, lon, value }

Examples:
- curl http://localhost:8080/api/health
- curl "http://localhost:8080/api/heatmap?metric=price&period=last30d"

Expected timings (dev):
- First call (cold, uncached): up to ~2s depending on dataset
- Subsequent calls (cached): <200ms
