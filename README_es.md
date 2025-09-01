# OPIS HeatMap MVP — Backend (Spring Boot + SQL + Redis)

## 🎯 Objetivo
Backend en **Spring Boot 3 (Java 21)** para exponer un API que alimente un dashboard de **heat maps** de precios y ventas de combustibles en EE.UU.

El enfoque es un **MVP monolítico**, pero diseñado con fronteras claras para escalar a microservicios en el futuro.

---

## ⚙️ Arquitectura

- **Spring Boot 3 + Gradle**
- **Base de datos relacional**: PostgreSQL (MVP local) y soporte para MSSQL (producción).
- **ORM / JPA** con **Flyway** para migraciones.
- **Redis** para caching de agregados.
- **Springdoc OpenAPI** para documentación de APIs.
- **Spring Batch o @Scheduled** para jobs de pre-aggregación.

Flujo:
```
Client (Dashboard) --> HeatMap API --> HeatMap Service --> Redis Cache
                                              │
                                      Aggregated Views / Batch Jobs
                                              │
                                      Station / Product / Price / Sales (DB)
```

---

## 🗄️ Modelo de Datos

**Station**
- id (PK), code, name, state, latitude, longitude  

**Product**
- id (PK), code, name, family  

**Price** (histórico de precios)
- id (PK), station_id (FK), product_id (FK), ts, price  

**Sales** (volumen vendido)
- id (PK), station_id (FK), product_id (FK), ts, volume, revenue?, tx_count?  

🔗 Relaciones:
- Station (1) ──< Price, Sales  
- Product (1) ──< Price, Sales  

---

## 🧩 HeatMap Component

Un **vertical slice** dentro del monolito que contiene:
- **Repository Layer** → acceso a entidades (Station, Product, Price, Sales).
- **Service Layer** → lógica de agregación (avg price, total volume, etc).
- **Cache Layer (Redis)** → resultados precalculados (`metric:period`).
- **API Layer (REST)**:
  - `GET /api/heatmap?metric=price|volume&period=last30d`

Ejemplo de respuesta:
```json
[
  { "state": "TX", "lat": 29.76, "lon": -95.36, "value": 2.15 },
  { "state": "IL", "lat": 41.87, "lon": -87.62, "value": 2.09 }
]
```

---

## 📊 Estrategia de Escalabilidad (2B+ filas)

- **Pre-aggregation**: materialized views o tablas resumen (ej. por día/estado/producto).
- **Batch/Scheduler**: refresco de agregados fuera de línea (ej. cada noche).
- **Indexación & particionado**: `(station_id, product_id, ts)` + particiones por mes/año.
- **Cache Redis**: dashboard consulta siempre resultados precalculados y cacheados.

---

## ❓ Preparación para la Entrevista

### 1. Monolith vs. Microservices
- Experiencia en ambos enfoques (Java EE monolitos y Spring Boot microservicios).
- Preferencia para MVP: **monolito modular** (faster delivery, menos overhead).
- Diseño con fronteras claras para futura migración a microservicios.

**Frase clave:**  
> “I like to start with a modular monolith, deliver value quickly, but design boundaries so that they can evolve into microservices when needed.”

---

### 2. Manejo de 2B rows
- Experiencia previa con sistemas de millones de registros/eventos.
- Estrategia: pre-aggregations en DB + batch jobs + Redis cache.
- El frontend **nunca** accede directamente a tablas crudas.

**Frase clave:**  
> “With billions of rows, I would never let the frontend query raw tables. I’d precompute aggregates, store them in views, and expose them through cached APIs.”

---

### 3. HeatMap Component
- Contiene repositorios, lógica de agregación, cache y API contract.
- Se apoya en frameworks: **Spring Batch**, **Redis**, **Flyway**, **OpenAPI**.
- Heavy lifting en DB/batch, runtime liviano y rápido.

**Frase clave:**  
> “The HeatMap component is a vertical slice: aggregation logic, caching, and API. Heavy lifting happens offline via batch jobs and materialized views.”

---

## 🚀 Roadmap MVP (para agentes de IA)

1. **Bootstrap**: Spring Boot app con Gradle.  
2. **DB migrations**: Flyway con tablas Station, Product, Price, Sales.  
3. **Seed Data**: ~50k ventas sintéticas para pruebas locales.  
4. **API**: implementar `/api/heatmap`.  
5. **Cache**: integración con Redis.  
6. **Aggregation**: empezar con lógica en memoria → migrar a SQL views.  
7. **Batch**: job programado para refrescar agregados.  
8. **Docs & Monitoring**: Swagger + Actuator.

---

## 🧭 Plan de Acción paso a paso
- Ver [PLAN_accion_es.md](./PLAN_accion_es.md) para commits y criterios de aceptación.

---

## ✅ Criterio de éxito del MVP
- App corre local con Docker (Postgres + Redis).  
- Endpoint `/api/heatmap` responde en < 1s gracias a cache.  
- DB lista para escalar a 2B filas con materialized views + batch.  


---

## 🧪 Desarrollo local con Docker Compose

Requisitos:
- Docker y Docker Compose
- JDK 21+
- Gradle wrapper (incluido)

Pasos:
1. Levantar la infraestructura (Postgres + Redis):
   - docker compose up -d
2. Ejecutar la aplicación (perfil dev por defecto):
   - ./gradlew bootRun
3. Verificar que esté funcionando:
   - curl http://localhost:8080/api/health
   - curl "http://localhost:8080/api/heatmap?metric=price&period=last30d"
4. Detener la infraestructura al finalizar:
   - docker compose down -v

Notas:
- Datasource dev: jdbc:postgresql://localhost:5432/heatmap (user/password: heatmap)
- Redis: localhost:6379
- Flyway corre migraciones al iniciar en perfil dev.


---

## 📚 Documentación de API

- Swagger UI: http://localhost:8080/swagger-ui.html (o /swagger-ui/index.html)
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Endpoints:
- GET /api/health
  - Respuesta: { "status": "UP", "commitSha": "<opcional>" }
- GET /api/heatmap?metric=price|volume&period=last30d
  - Respuesta: array de HeatPoint { state, lat, lon, value }

Ejemplos:
- curl http://localhost:8080/api/health
- curl "http://localhost:8080/api/heatmap?metric=price&period=last30d"

Tiempos esperados (dev):
- Primera llamada (frío, sin cache): hasta ~2s según dataset
- Llamadas posteriores (cacheado): <200ms
