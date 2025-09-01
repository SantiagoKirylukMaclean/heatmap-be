# Plan de Acción — OPIS HeatMap MVP (Backend)

Objetivo: construir de menor a mayor un backend en Spring Boot 3 (Java 21) para un dashboard de heat maps, partiendo de un MVP con un único controlador de health y evolucionando por commits pequeños y demostrables.

La app es un monolito modular listo para evolucionar a microservicios. Este plan también prepara respuestas a 3 preguntas típicas de entrevista (al final).

---

## Convenciones
- Rama: `main` estable, trabajo en ramas `feature/*` con PRs pequeños.
- Mensajes de commit: `feat|chore|refactor|test|docs|build(scope): mensaje`.
- Versionado: semántico (MVP `0.1.0`).

---

## Paso 0 — Bootstrap (ya presente)
- Estructura mínima de Spring Boot con Gradle.
- Aceptación: `./gradlew bootRun` levanta y `/actuator/health` no existe aún.

---

## Paso 1 — MVP mínimo: Health Controller
- Cambios:
  - Añadir controlador REST `GET /api/health` que devuelva `{ "status": "UP" }` y `commitSha` si existe.
  - Añadir test `HealthControllerTests` (MockMvc) con 200 OK.
  - Habilitar Actuator básico solo para `health` (dev).
- Commit(s):
  - `feat(api): add /api/health endpoint with basic info`
  - `test(api): add HealthController tests`
- Aceptación: `curl :8080/api/health` responde 200 en local.

---

## Paso 2 — Calidad y utilidades dev
- Cambios:
  - Dependencias: springdoc-openapi-ui, actuator en dev, lombok opcional.
  - Config perfiles: `application.yml` + `application-dev.yml`.
  - Log json (opcional) y banner.
- Commits:
  - `build(docs): add springdoc and swagger-ui`
  - `chore(config): add dev profile and structured logging`
- Aceptación: Swagger UI en `/swagger-ui.html` muestra `/api/health`.

---

## Paso 3 — Persistencia base (Flyway + JPA)
- Cambios:
  - Agregar Flyway, datasource Postgres (docker) y perfiles `dev`/`test`.
  - Migración `V1__base_schema.sql` con tablas: `station`, `product`, `price`, `sales`.
  - Entidades JPA básicas y repos read-only.
- Commits:
  - `build(db): add flyway and postgres config`
  - `feat(db): V1 base schema for station/product/price/sales`
  - `feat(db): add JPA entities and repositories`
- Aceptación: `./gradlew flywayMigrate` ok y app arranca.

---

## Paso 4 — Datos de ejemplo (semilla)
- Cambios:
  - Script `V2__seed_reference_data.sql` para `station` y `product`.
  - Cargador dev que inserte ~50k filas sintéticas en `sales` y `price`.
- Commits:
  - `feat(db): seed reference data for station and product`
  - `feat(dev): synthetic generator for price/sales dev dataset`
- Aceptación: consultas simples devuelven filas; tiempos razonables.

---

## Paso 5 — Vertical HeatMap (v1 en memoria)
- Cambios:
  - DTO `HeatPoint { state, lat, lon, value }`.
  - Servicio que agrega en memoria por estado: avg(price) o sum(volume) últimos 30 días.
  - Controlador `GET /api/heatmap?metric=price|volume&period=last30d`.
  - Tests de servicio y controller.
- Commits:
  - `feat(heatmap): add DTOs and service with in-memory aggregation`
  - `feat(api): expose /api/heatmap endpoint`
  - `test(heatmap): service and controller tests`
- Aceptación: endpoint responde <2s sobre dataset dev.

---

## Paso 6 — Caching con Redis
- Cambios:
  - Agregar Redis (docker-compose) y Spring Data Redis.
  - Cachear respuestas por clave `heatmap:{metric}:{period}` con TTL configurable.
- Commits:
  - `build(cache): add redis dependency and config`
  - `feat(cache): cache heatmap responses by metric and period`
- Aceptación: segunda llamada a `/api/heatmap` <200ms.

---

## Paso 7 — Agregación en SQL (materialized view / summary table)
- Cambios:
  - `V3__summary_sales_price.sql`: vista materializada o tabla resumen por día-estado-producto con índices.
  - Servicio cambia a consultar la vista en vez de tablas crudas.
- Commits:
  - `feat(db): add daily state/product summary view with indexes`
  - `refactor(heatmap): read aggregates from summary view`
- Aceptación: consultas consistentes y más rápidas; tests verdes.

---

## Paso 8 — Batch/Scheduler para refresco de agregados
- Cambios:
  - Spring Batch o `@Scheduled` que recalcule/resume incrementalmente.
  - Manejo de datos fuera de orden: upserts por ventana (p.ej., últimos N días) y marca de watermarks.
- Commits:
  - `feat(batch): scheduled job to refresh aggregates incrementally`
  - `feat(db): add upsert and watermark strategy`
- Aceptación: job corre localmente, idempotente y monitoreado por Actuator.

---

## Paso 9 — Observabilidad y seguridad básica
- Cambios:
  - Actuator endpoints (health, metrics, info) y `info.commit.id`.
  - Logs de request (excl. PII) y rate limit básico opcional.
- Commits:
  - `feat(obs): expose actuator metrics and info`
  - `chore(logging): add request logging`
- Aceptación: `/actuator/health` y `/actuator/info` operativos.

---

## Paso 10 — Docker Compose para dev
- Cambios:
  - `docker-compose.yml` con Postgres y Redis.
  - Perfilar app para apuntar a servicios de compose.
- Commits:
  - `chore(dev): add docker-compose for postgres and redis`
  - `docs(dev): usage instructions`
- Aceptación: `docker compose up` + `./gradlew bootRun` y todo funciona.

---

## Paso 11 — Documentación abierta
- Cambios:
  - Describir `/api/health` y `/api/heatmap` con anotaciones OpenAPI.
  - README: flujos, ejemplos y tiempos esperados.
- Commits:
  - `docs(api): enhance OpenAPI for heatmap endpoints`
  - `docs(readme): add examples and diagrams`
- Aceptación: Swagger UI correcta y usable.

---

## Paso 12 — Tests y rendimiento
- Cambios:
  - Tests de repos con datos dev (Testcontainers opcional).
  - JMH o simple medidor de latencia para endpoint cacheado.
- Commits:
  - `test(repo): add repository tests with dev dataset`
  - `test(perf): add latency check for cached heatmap`
- Aceptación: CI local verde; p95 < 1s cacheado.

---

## Entrevista: respuestas clave
1) Monolito vs Microservicios
- Preferencia inicial: monolito modular por velocidad y simplicidad operativa; diseñar límites para escalar a microservicios cuando haya dolor claro (equipos independientes, escalamiento diferencial, ritmos de despliegue).

2) 2B filas y datos fuera de orden
- Pre-aggregate en DB (vistas materializadas / tablas resumen particionadas) + jobs incrementales.
- Manejo de desorden temporal: ventanas deslizantes con upsert y watermarks; idempotencia; re-cálculo periódico.
- Cache Redis para servir dashboards rápido; el frontend nunca consulta tablas crudas.

3) Componente HeatMap
- Contiene repos, servicio de agregación, capa de cache, API REST, y job de refresco.
- Frameworks: Spring Data, Flyway, Redis, Spring Batch/Scheduler, OpenAPI, Actuator.

---

## Éxito del MVP
- `/api/health` y `/api/heatmap` funcionando y documentados.
- Agregados servidos desde vista/tabla resumen y cacheados.
- Docker compose con Postgres + Redis para local.
- Tests y tiempos objetivo: <1s para respuestas cacheadas.
