# Observability stack (Prometheus + Grafana)

This folder contains a ready-to-run Prometheus + Grafana stack for your microservices.

What you get:
- Prometheus scraping metrics from:
  - api-gateway (8765)
  - user-service (8000)
  - poll-service (8083)
  - limits-service (8084)
  - naming-server (Eureka, 8761)
- Grafana pre-provisioned with a Prometheus datasource and a "Microservices Overview" dashboard.

## Prerequisites
- Docker Desktop installed and running on Windows
- Your services running locally and exposing `/actuator/prometheus` (Spring Boot)
  - Add dependencies:
    - `org.springframework.boot:spring-boot-starter-actuator`
    - `io.micrometer:micrometer-registry-prometheus`
  - Add properties:
    - `management.endpoints.web.exposure.include=health,info,prometheus`
    - `management.endpoint.prometheus.enabled=true`

## How to run

Open Command Prompt and run:

```
cd C:\Users\Alala\projects\polling-app-with-limits-service\observability
docker compose up -d
```

Check containers:
```
docker compose ps
```

Open UIs:
- Prometheus: http://localhost:9090 (Targets page: http://localhost:9090/targets)
- Grafana: http://localhost:3000 (login admin/admin)

The Grafana dashboard "Microservices Overview" is auto-loaded (home ➜ Dashboards).

## Adjusting targets
Prometheus uses `host.docker.internal` to scrape Windows-host services from containers. If your ports differ, update `prometheus/prometheus.yml` accordingly and run:
```
docker compose restart prometheus
```

## Verify metrics quickly
Use curl from Windows:
```
curl -s http://localhost:8765/actuator/prometheus > NUL
curl -s http://localhost:8084/actuator/prometheus > NUL
curl -s http://localhost:8083/actuator/prometheus > NUL
curl -s http://localhost:8000/actuator/prometheus > NUL
```
Then open Prometheus ➜ Status ➜ Targets and ensure all show `UP`.

## Troubleshooting
- Target DOWN in Prometheus:
  - Confirm service is running and `/actuator/prometheus` returns 200
  - Confirm the port in `prometheus.yml` matches your service
- Grafana shows no data:
  - Wait ~10–20 seconds after starting containers
  - Ensure Prometheus targets are UP
- Naming server (Eureka) 404 for `/actuator/prometheus`:
  - It means metrics endpoint isn’t exposed; add Actuator + Prometheus deps and the properties above, then restart naming-server.

## Stop and remove
```
docker compose down
```

