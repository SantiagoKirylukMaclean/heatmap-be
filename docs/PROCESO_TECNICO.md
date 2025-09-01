PROCESO TÉCNICO — OPIS HeatMap MVP (Backend)

Resumen ejecutivo
- Objetivo: entregar un backend Spring Boot 3 (Java 21) para un dashboard de heat maps de precios/volúmenes de combustibles en EEUU, priorizando entrega incremental, rendimiento y mantenibilidad.
- Enfoque: monolito modular con arquitectura hexagonal, preparado para evolucionar a microservicios cuando haya razones operativas claras.
- Resultado actual: API /api/health y /api/heatmap expuestas con documentación OpenAPI, agregaciones por estado servidas desde una tabla/vista de resumen en SQL y cacheadas en Redis, con job de refresco programado. Perfiles dev/test y base de datos inicializada con Flyway.

Contexto temporal
- Ventana de trabajo según git log: 2025-08-30 a 2025-09-01.

Convenciones y prácticas
- Versionado de cambios: commits pequeños y descriptivos siguiendo convención tipo conventional commits.
- Arquitectura: hexagonal (puertos y adaptadores) con capas api (controllers), application (servicios), domain (modelos), infrastructure (cache, summary SQL, batch, observabilidad, adapters JDBC/JPA).
- Estándares Spring: constructor injection, perfiles en application.yml, Actuator, OpenAPI, Spring Cache con Redis, Flyway para migraciones, JPA para entidades base.

Arquitectura lógica
- API: HealthController y HeatmapController.
- Aplicación: HeatmapService (interface) y DefaultHeatmapService (servicio principal).
- Dominio: HeatPoint, Metric, Period.
- Infraestructura: 
  - summary SQL: SummaryRepository y DefaultSummaryRepository (JdbcTemplate) para agregaciones centralizadas.
  - cache: RedisCacheConfig, HeatmapCacheProperties.
  - batch/scheduler: SummaryRefreshScheduler, SummaryRefreshProperties.
  - observabilidad: GitInfoContributor (info.commit.id), logging estructurado.
  - puertos/adaptadores: StationQueryPort/DefaultStationQueryAdapter, PriceQueryPort/JdbcPriceQueryAdapter, SalesQueryPort/JdbcSalesQueryAdapter.

Línea de tiempo y mapeo a commits (hash corto | fecha | descripción)
- 3f4075b | 2025-08-30 | HeatMap: initial Spring Boot setup with Gradle
- aad4521 | 2025-08-30 | Add HealthController with actuator integration and basic tests
- 2ef6e64 | 2025-08-30 | Add configuration files and logging enhancements
- ea53221 | 2025-08-31 | Add initial database schema, JPA entities, repositories, and profiles setup
- fc67355 | 2025-08-31 | Add synthetic data loader, seed migration, and modular package restructuring
- 9e1293c | 2025-08-31 | Add heatmap service and API endpoints with JPA integration
- d04ffa6 | 2025-08-31 | Add Redis caching for heatmap service with TTL configuration
- b41f13b | 2025-09-01 | Add SQL-based summary repository and enhance heatmap service for aggregation optimization
- a37cf09 / f688ce2 | 2025-09-01 | Introduce scheduled summary refresh with SQL-based batch processing
- 28c55da | 2025-09-01 | Enable conditional Redis caching and improve local setup documentation
- 275b32b | 2025-09-01 | Add OpenAPI annotations for API documentation
- 243776d | 2025-09-01 | Add unit tests for summary repository and heatmap performance
- 84d2e43 | 2025-09-01 | Introduce query adapters and refactor heatmap service for port-driven architecture

Paso a paso con razones técnicas (alineado con PLAN_accion_es.md)

Paso 0 — Bootstrap
- Acción: proyecto Spring Boot 3 con Gradle y estructura mínima.
- Razón: base ejecutable rápida para iterar; facilita CI/CD y pruebas locales.
- Evidencia: commit 3f4075b.

Paso 1 — Health Controller
- Acción: GET /api/health con { status: UP, commitSha? }, integración Actuator e info.commit.id.
- Razón: punto de vida y trazabilidad de build.
- Evidencia: aad4521. Controlador en com.puetsnao.heatmap.api.HealthController.

Paso 2 — Config y utilidades dev
- Acción: application.yml y perfiles; logging; banner opcional.
- Razón: separar dev/test/prod y mejorar observabilidad local.
- Evidencia: 2ef6e64. Archivos en src/main/resources/application*.yml y logback-spring.xml.

Paso 3 — Persistencia base (Flyway + JPA)
- Acción: Flyway con V1__base_schema.sql, entidades JPA y repos read-only; perfiles DB.
- Razón: control de esquema por migraciones, modelo relacional explícito.
- Evidencia: ea53221. Entidades en price/product/sales/station e interfaces Repository.

Paso 4 — Datos de ejemplo (semilla)
- Acción: V2__seed_reference_data.sql y cargador sintético DevDatasetLoader para poblar sales/price.
- Razón: dataset realista para validar tiempos y lógicas de agregación.
- Evidencia: fc67355. Loader en com.puetsnao.heatmap.dev.DevDatasetLoader.

Paso 5 — HeatMap v1 (inicial)
- Acción: endpoint GET /api/heatmap y servicio de agregación. Uso de Station/Price/Sales.
- Razón: entregar valor funcional temprano; validar contrato y latencias iniciales.
- Evidencia: 9e1293c. Clases: HeatmapController, HeatmapService, DefaultHeatmapService.

Paso 6 — Caching con Redis
- Acción: integrar Redis (Spring Cache), TTL parametrizable (HeatmapCacheProperties), configuración (RedisCacheConfig), key basada en metric y period.
- Razón: respuesta del dashboard <200ms en llamadas repetidas; aliviar presión de DB.
- Evidencia: d04ffa6 y 28c55da (condiciones para habilitar/deshabilitar cache en entornos), README actualizado.

Paso 7 — Agregaciones en SQL (summary)
- Acción: repositorio SQL (DefaultSummaryRepository con JdbcTemplate) consultando tabla/vista de resumen (daily_state_product_summary). Migraciones V3__summary_sales_price.sql y V4__summary_table_and_watermark.sql.
- Razón: desplazar el trabajo pesado a la base, mejorar consistencia y tiempos bajo carga; facilitar índices y particionado.
- Evidencia: b41f13b. Interfaz SummaryRepository introducida y consumida por DefaultHeatmapService.

Paso 8 — Batch/Scheduler para refresco
- Acción: job programado (SummaryRefreshScheduler) y propiedades (SummaryRefreshProperties) para recalcular agregados por ventana, estrategia de watermark/upsert.
- Razón: mantener summary actualizado con datos fuera de orden y lograr idempotencia.
- Evidencia: a37cf09 / f688ce2. Integración con Actuator para monitoreo del job.

Paso 9 — Observabilidad y seguridad básica
- Acción: Actuator health/metrics/info, GitInfoContributor para info.commit.id; logging de request; RateLimitProperties base.
- Razón: visibilidad operativa y preparación para controles de tráfico.
- Evidencia: HealthController, GitInfoContributor, logback-spring.xml.

Paso 10 — Docker Compose para dev
- Acción: docker-compose.yml con Postgres y Redis; perfiles apuntando a servicios locales.
- Razón: onboarding rápido y entorno reproducible para el equipo.
- Evidencia: docker-compose.yml y README.md (sección desarrollo local).

Paso 11 — Documentación abierta
- Acción: anotaciones OpenAPI en controladores; Swagger UI.
- Razón: contrato autodescubible para integradores y QA.
- Evidencia: 275b32b. Endpoints listados en README.md.

Paso 12 — Tests y rendimiento
- Acción: pruebas unitarias y de rendimiento para repositorio de summary y para Heatmap; MockMvc para API.
- Razón: garantizar no regresiones y validar p95 < 1s cacheado.
- Evidencia: 243776d y src/test/java/... HeatmapPerformanceTests, DefaultSummaryRepositoryTests, HealthControllerTests, HeatmapControllerTests.

Diseño detallado por componentes

API (controladores)
- HealthController
  - Ruta: /api/health (GET). Devuelve status=UP y commitSha si está disponible en propiedades (app.commit-sha o info.commit.id). Registra evento y documenta con OpenAPI.
- HeatmapController
  - Ruta: /api/heatmap (GET). Parámetros metric=price|volume y period=last30d. Transforma cadenas a enums Metric y Period y delega en HeatmapService.

Aplicación
- HeatmapService (interfaz) y DefaultHeatmapService (servicio):
  - Patrón ports & adapters: depende de StationQueryPort, PriceQueryPort, SalesQueryPort y opcionalmente de SummaryRepository.
  - Selección de origen de datos: si hay SummaryRepository, usa agregaciones SQL; si no, cae a puertos de lectura directa (útil en tests).
  - Cache: anotación @Cacheable(cacheNames="heatmap", key="'heatmap:v2:' + metric + ':' + period") para respuestas repetidas.
  - Cálculo de centroides: promedia lat/lon por estado usando StationQueryPort. Mapeo a HeatPoint ordenado por estado.

Dominio
- HeatPoint (record con state, lat, lon, value) como DTO de salida.
- Metric y Period como enums con parsing robusto desde cadenas.

Infraestructura — Summary SQL
- SummaryRepository y DefaultSummaryRepository (JdbcTemplate):
  - averagePriceByState(fromDate, toDate): suma price_sum y price_count por estado y calcula promedio.
  - totalVolumeByState(fromDate, toDate): suma volume_sum por estado.
  - Origen: tabla/vista daily_state_product_summary poblada por migraciones V3/V4 y refrescada por scheduler.

Infraestructura — Batch/Scheduler
- SummaryRefreshScheduler y SummaryRefreshProperties: tarea programada que recalcula ventanas recientes aplicando estrategia de watermark y upsert para datos fuera de orden. Expuesto y monitorizable via Actuator.

Infraestructura — Cache
- RedisCacheConfig: configuración de cache manager Redis y serialización; TTL configurado en HeatmapCacheProperties.
- HeatmapCacheProperties: propiedades type-safe para habilitar/deshabilitar cache y definir expiración.

Infraestructura — Observabilidad
- GitInfoContributor: contribuye info.commit.id para /actuator/info y es reutilizado por HealthController para exponer commitSha.
- logback-spring.xml: niveles de log y pattern de salida consistente.

Puertos y adaptadores (hexagonal)
- StationQueryPort y DefaultStationQueryAdapter: lectura de ubicaciones de estaciones.
- PriceQueryPort y JdbcPriceQueryAdapter: agregación de precio promedio por estado (en modo fallback sin summary SQL).
- SalesQueryPort y JdbcSalesQueryAdapter: agregación de volumen total por estado (fallback sin summary SQL).
- Repositorios JPA (read-only) para entidades base: PriceEntity, SaleEntity, ProductEntity, StationEntity.

Datos y migraciones
- V1__base_schema.sql: tablas station, product, price, sales.
- V2__seed_reference_data.sql: datos de referencia para station y product.
- V3__summary_sales_price.sql: vista materializada/tablas de resumen para agregados por día-estado-producto con índices.
- V4__summary_table_and_watermark.sql: refuerzo de estrategia de resumen y watermark para procesar datos fuera de orden.

Documentación y contrato
- OpenAPI: anotaciones en controladores, Swagger UI en /swagger-ui.html, JSON en /v3/api-docs.
- README.md y README-onepager_en.md: visión, arquitectura, endpoints y pasos de ejecución.
- PLAN_accion_es.md: guion paso a paso y criterios de aceptación usados como checklist del proyecto.

Pruebas
- HealthControllerTests y HeatmapControllerTests: verificación del contrato HTTP con MockMvc.
- DefaultSummaryRepositoryTests: verificación de consultas SQL de agregación con dataset dev.
- HeatmapPerformanceTests: validación de latencia esperada con cache (objetivo <200ms tras precalentamiento).

Rendimiento y escalabilidad
- Razón principal de diseño: con volúmenes de 2B+ filas, las consultas del dashboard deben servirse desde agregados precomputados.
- Estrategia:
  - Pre-aggregación en DB (vistas/materialized/tabla resumen particionada por fecha y claves de negocio).
  - Jobs incrementales con ventanas deslizantes y watermarks para idempotencia.
  - Redis como caché frente al API para latencias sub-200ms.
  - Índices en (station_id, product_id, ts) y particionado temporal para mantener tiempos logarítmicos.

Lecciones y trade-offs
- Entregar primero la API con agregación simple ayudó a fijar el contrato, pero migrar a SQL/summary fue clave para rendimiento.
- Redis reduce p95, pero exige invalidación/TTL adecuada; se prefirió TTL fija y clave por metric:period para simplicidad del MVP.
- Hexagonalidad permitió cambiar la fuente de agregados (puertos vs summary SQL) sin tocar el API.
- Batch vs vistas materializadas: se eligió scheduler + tabla/vista por control e idempotencia; alternativas (trigger, streaming) quedan abiertas.

Ruta a microservicios
- Limitar el bounded context HeatMap con contratos claros (puertos) permite extraer en el futuro:
  - Servicio de Agregación (batch) separado con su ciclo de despliegue.
  - Servicio de API de Lectura escalable horizontalmente detrás de caché.
- Compartir esquema vía CDC o colas si se descomponen bounded contexts adicionales.

Cómo reproducir (dev)
- Requisitos: Docker + Compose, JDK 21+, Gradle wrapper.
- Pasos:
  - docker compose up -d
  - ./gradlew bootRun
  - curl http://localhost:8080/api/health
  - curl "http://localhost:8080/api/heatmap?metric=price&period=last30d"
- Perfiles: dev por defecto. Datasource Postgres y Redis locales según README.md.

Criterios de aceptación (síntesis)
- /api/health responde 200 con status=UP y commitSha opcional.
- /api/heatmap devuelve lista de HeatPoint ordenados por estado en <2s en frío y <200ms cacheado.
- Migraciones Flyway aplican en arranque dev; dataset dev cargado.
- Job de refresco ejecuta y es idempotente en ventana.

Archivos clave y responsabilidades
- com.puetsnao.heatmap.api.HealthController — endpoint de vida y commitSha.
- com.puetsnao.heatmap.api.HeatmapController — API de heatmap.
- com.puetsnao.heatmap.application.HeatmapService — contrato de servicio.
- com.puetsnao.heatmap.application.DefaultHeatmapService — lógica de agregación, centroides y cache.
- com.puetsnao.heatmap.infrastructure.summary.SummaryRepository — contrato de agregación SQL.
- com.puetsnao.heatmap.infrastructure.summary.DefaultSummaryRepository — queries SQL a daily_state_product_summary.
- com.puetsnao.heatmap.infrastructure.batch.SummaryRefreshScheduler — job programado de refresco.
- com.puetsnao.heatmap.infrastructure.cache.RedisCacheConfig — configuración de Redis cache manager.
- com.puetsnao.heatmap.infrastructure.cache.HeatmapCacheProperties — TTL y feature toggle de cache.
- com.puetsnao.heatmap.infrastructure.observability.GitInfoContributor — commit id para Actuator.
- com.puetsnao.heatmap.dev.DevDatasetLoader — carga sintética de datos dev.

Riesgos y mitigaciones
- Datos fuera de orden: mitigado con ventana de recomputación y watermark.
- Cardinalidad alta: mitigado con particionado e índices en summary.
- Coherencia cache: TTL controlado y claves segmentadas por métrica/período.

Próximos pasos sugeridos
- Añadir Testcontainers para pruebas de repositorio aisladas del entorno.
- Cargar dataset más grande para pruebas de stress.
- Incorporar métricas de negocio en Actuator (micrometer) y dashboards de observabilidad.
- Evaluar materialized views nativas de Postgres para ciertos agregados con refresh concurrente.

Glosario
- Watermark: marca de agua temporal para manejar eventos que llegan tarde.
- Upsert: inserción con actualización si existe (merge), útil para idempotencia.
- Bounded Context: delimitación conceptual del dominio que agrupa modelos y reglas coherentes.
