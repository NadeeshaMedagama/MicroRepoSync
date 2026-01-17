# 📊 Monitoring System Quick Reference

## Overview
A complete monitoring solution has been implemented for the RepoSync microservices using **Prometheus** and **Grafana**, following **SOLID principles**.

## 🚀 Quick Start

```bash
# 1. Build the monitoring service
mvn clean package -pl monitoring-service -am -DskipTests

# 2. Start monitoring stack
./docs/scripts/start-monitoring.sh

# Or manually
docker-compose up -d monitoring-service prometheus grafana
```

## 🌐 Access URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| **Grafana** | http://localhost:3000 | admin/admin |
| **Prometheus** | http://localhost:9090 | - |
| **Monitoring API** | http://localhost:8085/api/monitoring | - |

## 📡 Services Monitored

All microservices expose Prometheus metrics at `/actuator/prometheus`:

- **GitHub Service** - Port 8081
- **Document Processor** - Port 8082
- **Embedding Service** - Port 8083
- **Milvus Service** - Port 8084
- **Orchestrator** - Port 8086
- **Monitoring Service** - Port 8085

## 🔍 Key API Endpoints

```bash
# System health
curl http://localhost:8085/api/monitoring/health

# All services health
curl http://localhost:8085/api/monitoring/services/health

# Unhealthy services
curl http://localhost:8085/api/monitoring/services/unhealthy

# Trigger health check
curl -X POST http://localhost:8085/api/monitoring/health/check
```

## 📊 Useful Prometheus Queries

```promql
# Service availability
up{job=~".*-service"}

# Request rate
rate(http_server_requests_seconds_count[5m])

# Memory usage %
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# CPU usage %
system_cpu_usage * 100

# 95th percentile latency
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (job, le))
```

## 🚨 Pre-configured Alerts

1. **ServiceDown** - Service down > 1 minute
2. **HighMemoryUsage** - Heap > 85%
3. **CriticalMemoryUsage** - Heap > 95%
4. **HighCPUUsage** - CPU > 80%
5. **HighErrorRate** - Error rate > 10%
6. **FrequentGC** - Excessive garbage collection

## 📈 Grafana Dashboard Panels

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
Microservices → Monitoring Service → Prometheus → Grafana
    ↓               ↓
  Metrics      Health Checks
```

## 🐳 Docker Commands

```bash
# Start all services
docker-compose up -d

# Start only monitoring
docker-compose up -d monitoring-service prometheus grafana

# View logs
docker-compose logs -f monitoring-service
docker-compose logs -f prometheus
docker-compose logs -f grafana

# Stop monitoring
docker-compose stop monitoring-service prometheus grafana
```

## ☸️ Kubernetes Deployment

```bash
# Deploy monitoring stack
kubectl apply -f k8s/07-monitoring-service.yaml
kubectl apply -f k8s/07-prometheus.yaml
kubectl apply -f k8s/08-grafana.yaml

# Access services
kubectl port-forward -n reposync svc/prometheus 9090:9090
kubectl port-forward -n reposync svc/grafana 3000:3000
kubectl port-forward -n reposync svc/monitoring-service 8085:8085
```

## 🎯 SOLID Principles

- **S**ingle Responsibility - Each service has one clear purpose
- **O**pen/Closed - Extensible without modification
- **L**iskov Substitution - Interface-based design
- **I**nterface Segregation - Focused interfaces
- **D**ependency Inversion - Depends on abstractions

## 📚 Documentation

- **Complete Guide**: [MONITORING_GUIDE.md](./MONITORING_GUIDE.md)
- **Quick Start**: [MONITORING_QUICKSTART.md](./MONITORING_QUICKSTART.md)
- **Implementation**: [MONITORING_IMPLEMENTATION_SUMMARY.md](./MONITORING_IMPLEMENTATION_SUMMARY.md)

## ✅ Verification

```bash
# Verify configuration
./docs/scripts/verify-monitoring.sh

# Check services are running
docker ps | grep -E "monitoring|prometheus|grafana"

# Test endpoints
curl http://localhost:8085/actuator/health
curl http://localhost:9090/-/healthy
curl http://localhost:3000/api/health
```

## 🛠️ Troubleshooting

### Services not showing in Prometheus
- Check targets: http://localhost:9090/targets
- Verify service is running: `docker ps`
- Check actuator endpoint: `curl http://localhost:8081/actuator/prometheus`

### Grafana shows no data
- Verify Prometheus connection in Configuration → Data Sources
- Check time range in dashboard
- Verify Prometheus has data by running queries

## 📦 Files Created

```
monitoring-service/          # New microservice
monitoring/prometheus/       # Prometheus config
monitoring/grafana/         # Grafana dashboards
k8s/07-*.yaml              # Kubernetes manifests
docs/readmes/MONITORING_*   # Documentation
docs/scripts/start-monitoring.sh
```

## 🎉 Success!

Your monitoring system is now ready with:
- ✅ Real-time health monitoring
- ✅ Comprehensive metrics collection
- ✅ Professional dashboards
- ✅ Intelligent alerting
- ✅ SOLID architecture
- ✅ Full documentation

**Start monitoring now:**
```bash
./docs/scripts/start-monitoring.sh
```

Then access Grafana at http://localhost:3030 (admin/admin)

