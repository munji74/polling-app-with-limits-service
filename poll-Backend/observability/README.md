# Observability stack (Prometheus + Tempo + Grafana + Zipkin)

This folder runs metrics and tracing for the project.

Services and ports (from your host):
- Grafana UI: http://localhost:3000 (admin / admin)
- Prometheus UI/API: http://localhost:9090
- Tempo API (no UI): http://localhost:3200
  - Readiness: GET /ready
  - Status: GET /status
  - Metrics: GET /metrics
  - Get a trace (after one exists): GET /api/traces/{traceId}
  - OTLP ingest (from apps): gRPC on 4317, HTTP on 4318
- Zipkin UI: http://localhost:9411

## Start/stop

```cmd
cd observability
docker compose up -d

REM Check containers
docker compose ps

REM Show logs
docker compose logs --tail=100 tempo
```

## Verifying Tempo

```cmd
REM 200 OK when ready
curl -i http://localhost:3200/ready

REM Status payload
curl http://localhost:3200/status

REM Endpoint is alive (returns 400 because body is empty, which is fine)
curl -i -X POST http://localhost:4318/v1/traces -H "Content-Type: application/json" -d "{\"resourceSpans\":[]}"
```

Note: Seeing "404 page not found" at http://localhost:3200/ is expected. Tempo exposes APIs, not a homepage. Use Grafana to browse traces.

## Using Grafana with Tempo and Prometheus

1. Open Grafana at http://localhost:3000 (admin/admin).
2. Data sources are auto-provisioned:
   - Prometheus (default) → http://prometheus:9090
   - Tempo → http://tempo:3200
3. Explore → switch to Tempo data source → search for traces.
4. Build dashboards with PromQL from Prometheus.

## Configure your apps (Spring Boot example)

Set environment variables when running the service:

```cmd
set OTEL_SERVICE_NAME=poll-service
set OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
set OTEL_TRACES_EXPORTER=otlp
REM If exposing metrics for Prometheus scraping, ensure /actuator/prometheus is enabled.
```

Prometheus scrapes metrics from your service; Tempo receives traces from the OTLP exporter.

## Troubleshooting

- 404 at / (http://localhost:3200/): normal; use /ready, /status, /metrics or Grafana UI.
- Permission denied for Tempo storage: we run Tempo as root and use a named volume; if issues persist, recreate volume:

```cmd
cd observability
docker compose down
docker volume rm observability_tempo_data
docker compose up -d
```

- No traces in Grafana: verify your app exports to http://localhost:4318 or :4317, and that spans include `service.name`.
- Service map empty: you need both traces (Tempo) and metrics (Prometheus) with labels that map `service.name` → Prometheus job label.

## Zipkin

Zipkin expects spans via its collector endpoints:
- Zipkin JSON v2: POST http://localhost:9411/api/v2/spans
- OTLP (if using otel-collector normally; this image focuses on Zipkin format)

Spring Boot (Spring Cloud Sleuth 3.x legacy) can auto-send spans to Zipkin if you set:
```properties
management.tracing.sampling.probability=1.0
spring.zipkin.base-url=http://localhost:9411/
``` 
(If using modern OpenTelemetry instrumentation you typically send to Tempo instead; Zipkin here is for comparison and UI.)

### Comparing Tempo vs Zipkin
| Feature | Tempo | Zipkin |
|---------|-------|--------|
| Storage | Local block storage (scalable) | In-memory (here) / external backends | 
| Protocol ingest | OTLP (HTTP+gRPC), Jaeger, Zipkin via receivers (not enabled here) | Zipkin JSON | 
| UI | Via Grafana (rich correlation) | Built-in minimal UI | 
| Service graph | Grafana plugin using traces + metrics | Basic dependency graph | 

You can view the same application's spans in both systems if you export in both formats (e.g., OTLP to Tempo, Zipkin reporter to Zipkin) for learning.

## Simplified stack
We removed the OpenTelemetry Collector, Loki, and Promtail to reduce moving parts. Send OTLP traces directly to Tempo:
- OTLP HTTP: http://localhost:4318
- OTLP gRPC: http://localhost:4317

Zipkin remains available at http://localhost:9411 for comparison/testing. If you want a single OTLP endpoint to fan out to both Tempo and Zipkin later, we can re-add the collector.
