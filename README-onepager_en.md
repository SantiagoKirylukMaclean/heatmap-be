# OPIS HeatMap MVP — Backend (One-Pager)

## 🎯 Goal
MVP backend in **Spring Boot 3 (Java 21)** for heat maps of US fuel prices & sales.

---

## ⚙️ Architecture
- **Monolith** (modular, evolvable to microservices).  
- **DB**: PostgreSQL (MVP), MSSQL (prod).  
- **Flyway + JPA** for schema/migrations.  
- **Redis** for caching.  
- **Batch/Scheduler** for pre-aggregations.  
- **OpenAPI** docs, **Actuator** monitoring.

Flow: Dashboard → API → Service → Redis → Aggregated Views → DB

---

## 🗄️ Data Model
- **Station**: id, code, state, lat/lon.  
- **Product**: id, code, name.  
- **Price**: station_id, product_id, ts, price.  
- **Sales**: station_id, product_id, ts, volume, revenue?.  

Relations: Station/Product (1) ──< Price, Sales

---

## 🧩 HeatMap Component
- **Repositories** → raw data.  
- **Service** → aggregation logic.  
- **Cache** → Redis, key=`metric:period`.  
- **API** → `/api/heatmap`.

Output JSON: `{ state, lat, lon, value }`

---

## 📊 Scaling Strategy (2B rows)
- **Pre-aggregation**: materialized views / summary tables.  
- **Batch jobs**: refresh offline.  
- **Partitioning & indexes** on `(station_id, product_id, ts)`.  
- **Cache**: Redis for fast dashboards.

---

## ❓ Interview Prep

**1. Monolith vs Microservices**  
- Prefer modular monolith for MVP, evolve later.  
- “Boundaries ready for microservices.”

**2. Handling 2B rows**  
- Precompute aggregates, partition DB, cache APIs.  
- “Frontend never queries raw tables.”

**3. HeatMap Component**  
- Slice with repo, service, cache, API.  
- Supports batch jobs + DB views.  
- “Heavy lifting offline, API serves cached data.”

---

## ✅ MVP Success Criteria
- Local run with Docker (Postgres + Redis).  
- `/api/heatmap` responds < 1s (cached).  
- Schema & jobs ready for billions of rows.  
