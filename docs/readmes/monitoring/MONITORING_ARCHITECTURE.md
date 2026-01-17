# RepoSync Monitoring System - Complete Architecture

## System Overview Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CLIENT / USER INTERFACE                              │
│                                                                              │
│  Browser → http://localhost:3000 (Grafana Dashboards)                      │
│  CLI → curl http://localhost:8085/api/monitoring (Monitoring API)          │
│  Browser → http://localhost:9090 (Prometheus UI)                           │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                        VISUALIZATION LAYER                                   │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────┐        │
│  │                    GRAFANA (Port 3000)                          │        │
│  │  • Dashboard: Service Availability                             │        │
│  │  • Dashboard: Request Rate & Latency                           │        │
│  │  • Dashboard: Resource Usage (CPU, Memory)                     │        │
│  │  • Dashboard: Error Rates & GC Metrics                         │        │
│  │  • Auto-refresh: 10 seconds                                    │        │
│  │  • Datasource: Prometheus                                      │        │
│  └────────────────────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                      METRICS STORAGE & ALERTING LAYER                        │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────┐        │
│  │                  PROMETHEUS (Port 9090)                         │        │
│  │  • Scrapes metrics every 15 seconds                            │        │
│  │  • Evaluates alert rules every 15 seconds                      │        │
│  │  • Time-series database                                        │        │
│  │  • 8 Pre-configured alert rules                                │        │
│  │  • Retention: 15 days (default)                                │        │
│  │                                                                 │        │
│  │  Scrape Targets:                                               │        │
│  │  ├─ monitoring-service:8085/actuator/prometheus                │        │
│  │  ├─ github-service:8081/actuator/prometheus                    │        │
│  │  ├─ document-processor-service:8082/actuator/prometheus        │        │
│  │  ├─ embedding-service:8083/actuator/prometheus                 │        │
│  │  ├─ milvus-service:8084/actuator/prometheus                    │        │
│  │  └─ orchestrator-service:8080/actuator/prometheus              │        │
│  └────────────────────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↑
┌─────────────────────────────────────────────────────────────────────────────┐
│                       MONITORING AGGREGATION LAYER                           │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────┐        │
│  │             MONITORING SERVICE (Port 8085)                      │        │
│  │                                                                 │        │
│  │  Core Functions:                                               │        │
│  │  1. Health Check Scheduler (every 30s)                         │        │
│  │     ├─ Checks all service /actuator/health endpoints           │        │
│  │     ├─ Records response time                                   │        │
│  │     └─ Updates health status metrics                           │        │
│  │                                                                 │        │
│  │  2. Metrics Collection Scheduler (every 30s)                   │        │
│  │     ├─ Fetches important metrics from services                 │        │
│  │     ├─ Aggregates JVM, HTTP, and system metrics               │        │
│  │     └─ Registers in MeterRegistry                              │        │
│  │                                                                 │        │
│  │  3. REST API                                                   │        │
│  │     ├─ GET /api/monitoring/health                              │        │
│  │     ├─ GET /api/monitoring/services/health                     │        │
│  │     ├─ GET /api/monitoring/services/{name}/health              │        │
│  │     ├─ GET /api/monitoring/services/unhealthy                  │        │
│  │     └─ POST /api/monitoring/health/check                       │        │
│  │                                                                 │        │
│  │  SOLID Principles Implementation:                              │        │
│  │  • HealthCheckService (Interface)                              │        │
│  │  • HealthCheckServiceImpl                                      │        │
│  │  • MetricsCollectionService (Interface)                        │        │
│  │  • MetricsCollectionServiceImpl                                │        │
│  │  • MonitoringService (Coordinator)                             │        │
│  │  • MonitoringController (REST API)                             │        │
│  └────────────────────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↑
┌─────────────────────────────────────────────────────────────────────────────┐
│                        MICROSERVICES LAYER                                   │
│                                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   GitHub     │  │  Document    │  │  Embedding   │  │   Milvus     │  │
│  │   Service    │  │  Processor   │  │   Service    │  │   Service    │  │
│  │   :8081      │  │   :8082      │  │   :8083      │  │   :8084      │  │
│  │              │  │              │  │              │  │              │  │
│  │ Endpoints:   │  │ Endpoints:   │  │ Endpoints:   │  │ Endpoints:   │  │
│  │ /actuator/   │  │ /actuator/   │  │ /actuator/   │  │ /actuator/   │  │
│  │   health     │  │   health     │  │   health     │  │   health     │  │
│  │   prometheus │  │   prometheus │  │   prometheus │  │   prometheus │  │
│  │   metrics    │  │   metrics    │  │   metrics    │  │   metrics    │  │
│  │              │  │              │  │              │  │              │  │
│  │ Metrics:     │  │ Metrics:     │  │ Metrics:     │  │ Metrics:     │  │
│  │ • JVM Memory │  │ • JVM Memory │  │ • JVM Memory │  │ • JVM Memory │  │
│  │ • Threads    │  │ • Threads    │  │ • Threads    │  │ • Threads    │  │
│  │ • CPU        │  │ • CPU        │  │ • CPU        │  │ • CPU        │  │
│  │ • HTTP Req   │  │ • HTTP Req   │  │ • HTTP Req   │  │ • HTTP Req   │  │
│  │ • Errors     │  │ • Errors     │  │ • Errors     │  │ • Errors     │  │
│  │ • GC         │  │ • GC         │  │ • GC         │  │ • GC         │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                                              │
│  ┌──────────────────────────┐                                              │
│  │     Orchestrator         │                                              │
│  │      Service             │                                              │
│  │       :8080              │                                              │
│  │                          │                                              │
│  │ Coordinates all services │                                              │
│  │ Endpoints:               │                                              │
│  │ /actuator/health         │                                              │
│  │ /actuator/prometheus     │                                              │
│  │                          │                                              │
│  │ Metrics:                 │                                              │
│  │ • Workflow metrics       │                                              │
│  │ • Service calls          │                                              │
│  │ • Circuit breaker        │                                              │
│  │ • Standard JVM metrics   │                                              │
│  └──────────────────────────┘                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Metrics Flow

```
1. Microservices generate metrics using Micrometer
   ↓
2. Spring Boot Actuator exposes metrics at /actuator/prometheus
   ↓
3. Monitoring Service:
   - Periodically checks health endpoints
   - Collects important metrics
   - Registers aggregated metrics
   ↓
4. Prometheus:
   - Scrapes all services every 15 seconds
   - Stores metrics in time-series database
   - Evaluates alert rules
   ↓
5. Grafana:
   - Queries Prometheus for data
   - Renders dashboards
   - Displays real-time visualizations
```

## Alert Flow

```
1. Prometheus evaluates alert rules every 15 seconds
   ↓
2. If condition met for specified duration:
   - Alert fires
   - Status shown in Prometheus UI (http://localhost:9090/alerts)
   ↓
3. Can be integrated with Alertmanager (future enhancement):
   - Email notifications
   - Slack notifications
   - PagerDuty integration
   - Custom webhooks
```

## SOLID Principles in Monitoring Service

```
┌─────────────────────────────────────────┐
│     Dependency Inversion Principle      │
│                                         │
│  MonitoringService depends on:         │
│  • HealthCheckService (interface)      │
│  • MetricsCollectionService (interface)│
└─────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│   Interface Segregation Principle       │
│                                         │
│  Focused Interfaces:                   │
│  • HealthCheckService                  │
│    - checkServiceHealth()              │
│    - isServiceAvailable()              │
│                                         │
│  • MetricsCollectionService            │
│    - collectServiceMetrics()           │
│    - aggregateMetrics()                │
└─────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│   Single Responsibility Principle       │
│                                         │
│  Each class has one responsibility:    │
│  • HealthCheckServiceImpl → Health     │
│  • MetricsCollectionServiceImpl → Metrics│
│  • MonitoringService → Coordination    │
│  • MonitoringController → REST API     │
│  • ServiceHealth → Data model          │
│  • ServiceMetrics → Data model         │
└─────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│      Open/Closed Principle              │
│                                         │
│  Extensible without modification:      │
│  • Add new metric types               │
│  • Add new alert rules                │
│  • Add new services to monitor        │
│  • Add custom dashboards              │
└─────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│    Liskov Substitution Principle        │
│                                         │
│  Implementations are interchangeable:  │
│  • Can swap HealthCheckServiceImpl     │
│  • Can swap MetricsCollectionServiceImpl│
│  • Interface contracts are honored     │
└─────────────────────────────────────────┘
```

## Deployment Architecture

### Docker Compose

```
┌─────────────────────────────────────────┐
│         Docker Network                   │
│       reposync-network                   │
│                                          │
│  ┌────────────┐  ┌────────────┐        │
│  │  Services  │  │ Monitoring │        │
│  │            │  │  Service   │        │
│  │ (All 6)    │  │            │        │
│  └────────────┘  └────────────┘        │
│        ↑               ↑                 │
│        └───────┬───────┘                │
│                │                         │
│         ┌──────┴──────┐                 │
│         │ Prometheus  │                 │
│         └──────┬──────┘                 │
│                │                         │
│         ┌──────┴──────┐                 │
│         │   Grafana   │                 │
│         └─────────────┘                 │
│                                          │
│  Volumes:                                │
│  • prometheus-data                       │
│  • grafana-data                          │
└─────────────────────────────────────────┘
```

### Kubernetes

```
┌─────────────────────────────────────────┐
│         Namespace: reposync              │
│                                          │
│  Deployments:                            │
│  ├─ monitoring-service (1 replica)       │
│  ├─ prometheus (1 replica)               │
│  └─ grafana (1 replica)                  │
│                                          │
│  Services:                               │
│  ├─ monitoring-service:8085 (ClusterIP)  │
│  ├─ prometheus:9090 (ClusterIP)          │
│  └─ grafana:3030 (LoadBalancer)          │
│                                          │
│  ConfigMaps:                             │
│  ├─ prometheus-config                    │
│  └─ grafana-datasources                  │
│                                          │
│  RBAC:                                   │
│  ├─ ServiceAccount: prometheus           │
│  ├─ ClusterRole: prometheus              │
│  └─ ClusterRoleBinding: prometheus       │
└─────────────────────────────────────────┘
```

## Complete File Structure

```
Microservices_with_RepoSync/
├── monitoring-service/                    # New monitoring microservice
│   ├── src/
│   │   └── main/
│   │       ├── java/com/reposync/monitoring/
│   │       │   ├── MonitoringServiceApplication.java
│   │       │   ├── config/
│   │       │   │   ├── MetricsConfig.java
│   │       │   │   └── MonitoringProperties.java
│   │       │   ├── controller/
│   │       │   │   └── MonitoringController.java
│   │       │   ├── model/
│   │       │   │   ├── ServiceHealth.java
│   │       │   │   └── ServiceMetrics.java
│   │       │   └── service/
│   │       │       ├── HealthCheckService.java
│   │       │       ├── HealthCheckServiceImpl.java
│   │       │       ├── MetricsCollectionService.java
│   │       │       ├── MetricsCollectionServiceImpl.java
│   │       │       └── MonitoringService.java
│   │       └── resources/
│   │           └── application.yml
│   ├── Dockerfile
│   ├── pom.xml
│   └── target/
│       └── monitoring-service-1.0.0-SNAPSHOT.jar
│
├── monitoring/                            # Monitoring configurations
│   ├── prometheus/
│   │   ├── prometheus.yml                # Main Prometheus config
│   │   └── rules/
│   │       └── alerts.yml               # Alert rules
│   └── grafana/
│       ├── dashboards/
│       │   └── reposync-overview.json   # Main dashboard
│       └── provisioning/
│           ├── datasources/
│           │   └── prometheus.yml       # Prometheus datasource
│           └── dashboards/
│               └── dashboard.yml         # Dashboard provisioning
│
├── k8s/                                  # Kubernetes manifests
│   ├── 07-monitoring-service.yaml
│   ├── 07-prometheus.yaml               # Includes RBAC
│   └── 08-grafana.yaml
│
├── docs/
│   ├── readmes/
│   │   ├── MONITORING_GUIDE.md          # 400+ lines comprehensive guide
│   │   ├── MONITORING_QUICKSTART.md     # Quick reference
│   │   └── MONITORING_IMPLEMENTATION_SUMMARY.md
│   └── scripts/
│       ├── start-monitoring.sh          # Automated startup
│       └── verify-monitoring.sh         # Verification script
│
├── MONITORING_README.md                  # This quick reference
├── docker-compose.yml                    # Updated with monitoring
└── pom.xml                              # Updated parent POM
```

## Summary

This monitoring system provides:
- ✅ **Complete Observability**: Health, metrics, and visualization
- ✅ **SOLID Architecture**: Clean, maintainable, extensible code
- ✅ **Production Ready**: Docker, Kubernetes, alerts, documentation
- ✅ **Developer Friendly**: REST API, scripts, comprehensive docs
- ✅ **Best Practices**: Industry-standard tools (Prometheus, Grafana)
- ✅ **No Issues**: Successfully compiled and verified

**Start using it now:**
```bash
./docs/scripts/start-monitoring.sh
```

