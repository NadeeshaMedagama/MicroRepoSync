# RepoSync Monitoring System - Quick Reference

## 🚀 Quick Start

### Start Monitoring Stack
```bash
# Using the quick start script
./docs/scripts/start-monitoring.sh

# Or manually with docker-compose
docker-compose up -d monitoring-service prometheus grafana
```

### Access Interfaces
| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | admin/admin |
| Prometheus | http://localhost:9090 | - |
| Monitoring API | http://localhost:8085/api/monitoring | - |

## 📊 Key Metrics

### Check System Health
```bash
curl http://localhost:8085/api/monitoring/health
```

### View All Service Health
```bash
curl http://localhost:8085/api/monitoring/services/health
```

### View Unhealthy Services
```bash
curl http://localhost:8085/api/monitoring/services/unhealthy
```

## 🔍 Useful Prometheus Queries

| Metric | Query |
|--------|-------|
| Service Status | `up{job=~".*-service"}` |
| Request Rate | `rate(http_server_requests_seconds_count[5m])` |
| Memory Usage % | `(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100` |
| CPU Usage % | `system_cpu_usage * 100` |
| Error Rate | `rate(http_server_requests_seconds_count{status=~"5.."}[5m])` |
| 95th Percentile Latency | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (job, le))` |

## 📈 Grafana Dashboards

### Import Dashboard
1. Navigate to http://localhost:3000
2. Login with admin/admin
3. Click "+" → "Import"
4. Upload `monitoring/grafana/dashboards/reposync-overview.json`

### Key Panels
- Service Availability
- HTTP Request Rate
- Response Time (95th percentile)
- JVM Memory Usage
- CPU Usage
- Thread Count
- Error Rate
- Garbage Collection Time

## 🏗️ Architecture

```
┌─────────────────┐
│  Microservices  │
│   (All 6)       │──┐
└─────────────────┘  │
                     │ /actuator/prometheus
                     │
                     ▼
            ┌─────────────────┐
            │  Monitoring     │
            │   Service       │
            │   (Port 8085)   │
            └─────────────────┘
                     │
                     │ metrics
                     │
                     ▼
            ┌─────────────────┐
            │   Prometheus    │
            │   (Port 9090)   │
            └─────────────────┘
                     │
                     │ datasource
                     │
                     ▼
            ┌─────────────────┐
            │    Grafana      │
            │   (Port 3000)   │
            └─────────────────┘
```

## 🛠️ Configuration Files

| Component | Configuration File |
|-----------|-------------------|
| Monitoring Service | `monitoring-service/src/main/resources/application.yml` |
| Prometheus | `monitoring/prometheus/prometheus.yml` |
| Alert Rules | `monitoring/prometheus/rules/alerts.yml` |
| Grafana Datasource | `monitoring/grafana/provisioning/datasources/prometheus.yml` |
| Grafana Dashboards | `monitoring/grafana/dashboards/*.json` |

## 🚨 Alert Rules

| Alert | Condition | Severity |
|-------|-----------|----------|
| ServiceDown | Service down > 1 minute | Critical |
| HighMemoryUsage | Heap > 85% for 5 minutes | Warning |
| CriticalMemoryUsage | Heap > 95% for 2 minutes | Critical |
| HighCPUUsage | CPU > 80% for 5 minutes | Warning |
| HighErrorRate | Error rate > 10% | Critical |
| FrequentGC | GC > 5 times/sec for 5 min | Warning |

## 📦 Services Monitored

| Service | Port | Description |
|---------|------|-------------|
| GitHub Service | 8081 | GitHub API integration |
| Document Processor | 8082 | Document chunking |
| Embedding Service | 8083 | Azure OpenAI embeddings |
| Milvus Service | 8084 | Vector database |
| Orchestrator | 8080 | Workflow orchestration |
| Monitoring | 8085 | Health & metrics aggregation |

## 🐳 Docker Commands

### Start All Services
```bash
docker-compose up -d
```

### Start Only Monitoring Stack
```bash
docker-compose up -d monitoring-service prometheus grafana
```

### View Logs
```bash
docker-compose logs -f monitoring-service
docker-compose logs -f prometheus
docker-compose logs -f grafana
```

### Stop Monitoring
```bash
docker-compose stop monitoring-service prometheus grafana
```

### Restart Monitoring
```bash
docker-compose restart monitoring-service prometheus grafana
```

## ☸️ Kubernetes Commands

### Deploy Monitoring Stack
```bash
kubectl apply -f k8s/01-namespace-config.yaml
kubectl apply -f k8s/07-monitoring-service.yaml
kubectl apply -f k8s/07-prometheus.yaml
kubectl apply -f k8s/08-grafana.yaml
```

### Check Status
```bash
kubectl get pods -n reposync
kubectl get svc -n reposync
```

### Access Services (Port-Forward)
```bash
kubectl port-forward -n reposync svc/prometheus 9090:9090
kubectl port-forward -n reposync svc/grafana 3000:3000
kubectl port-forward -n reposync svc/monitoring-service 8085:8085
```

### View Logs
```bash
kubectl logs -n reposync -l app=monitoring-service -f
kubectl logs -n reposync -l app=prometheus -f
kubectl logs -n reposync -l app=grafana -f
```

## 🔧 Troubleshooting

### Services Not Showing in Prometheus

1. Check Prometheus targets: http://localhost:9090/targets
2. Verify service is running: `docker ps`
3. Check actuator endpoint: `curl http://localhost:8081/actuator/prometheus`

### Grafana Shows No Data

1. Verify Prometheus connection in Configuration → Data Sources
2. Check time range in dashboard
3. Run query in Prometheus UI to verify data exists

### High Memory Alerts

1. Check service logs: `docker logs <container-name>`
2. Adjust JVM heap: Add `-Xmx512m` to ENTRYPOINT in Dockerfile
3. Restart service: `docker-compose restart <service-name>`

## 📚 Documentation

- **Full Guide**: [MONITORING_GUIDE.md](./MONITORING_GUIDE.md)
- **Architecture**: See MONITORING_GUIDE.md for detailed architecture
- **SOLID Principles**: See MONITORING_GUIDE.md for implementation details

## 🎯 SOLID Principles

The monitoring system follows SOLID principles:

- **S**: Single Responsibility - Each service has one clear purpose
- **O**: Open/Closed - Extensible without modification
- **L**: Liskov Substitution - Interfaces can be swapped
- **I**: Interface Segregation - Focused interfaces
- **D**: Dependency Inversion - Depends on abstractions

## 📞 Support

For detailed information, see [MONITORING_GUIDE.md](./MONITORING_GUIDE.md)

