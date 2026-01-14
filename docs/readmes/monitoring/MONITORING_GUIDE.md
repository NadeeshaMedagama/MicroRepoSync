# RepoSync Monitoring System

## Overview

This document describes the comprehensive monitoring system implemented for the RepoSync microservices application using Prometheus and Grafana, following SOLID principles and microservices architecture patterns.

## Architecture

### Components

1. **Monitoring Service** (Port 8085)
   - Custom microservice for health checking and metrics aggregation
   - Follows SOLID principles with clean separation of concerns
   - Periodically checks health of all microservices
   - Exposes aggregated metrics to Prometheus

2. **Prometheus** (Port 9090)
   - Metrics collection and storage
   - Scrapes metrics from all services every 15 seconds
   - Implements alerting rules
   - Time-series database for metrics

3. **Grafana** (Port 3000)
   - Metrics visualization
   - Pre-configured dashboards
   - Connected to Prometheus as data source
   - Default credentials: admin/admin

## Services Being Monitored

All microservices expose metrics on the `/actuator/prometheus` endpoint:

- **GitHub Service** (Port 8081)
- **Document Processor Service** (Port 8082)
- **Embedding Service** (Port 8083)
- **Milvus Service** (Port 8084)
- **Orchestrator Service** (Port 8080)
- **Monitoring Service** (Port 8085)

## Metrics Collected

### JVM Metrics
- **Memory Usage**: Heap and non-heap memory utilization
- **Thread Count**: Active, daemon, and peak threads
- **Garbage Collection**: GC pause time and frequency
- **Class Loading**: Loaded and unloaded classes

### Application Metrics
- **HTTP Requests**: Request rate, response time, status codes
- **CPU Usage**: System and process CPU utilization
- **Error Rates**: 4xx and 5xx error frequencies
- **Custom Business Metrics**: Service-specific metrics

### Health Metrics
- **Service Availability**: Up/Down status
- **Response Time**: Health check latency
- **Dependency Health**: Database, external API status

## Alert Rules

The system includes predefined alerts:

1. **ServiceDown**: Triggers when a service is down for > 1 minute
2. **HighMemoryUsage**: Alerts when heap usage > 85% for 5 minutes
3. **CriticalMemoryUsage**: Critical alert when heap usage > 95%
4. **HighCPUUsage**: Warns when CPU usage > 80% for 5 minutes
5. **HighErrorRate**: Alerts on > 10% error rate
6. **FrequentGarbageCollection**: Warns on excessive GC activity

## Getting Started

### Using Docker Compose

1. **Start all services including monitoring:**
```bash
docker-compose up -d
```

2. **Access the monitoring interfaces:**
   - Prometheus: http://localhost:9090
   - Grafana: http://localhost:3000
   - Monitoring Service API: http://localhost:8085/api/monitoring

3. **View metrics in Prometheus:**
   - Navigate to http://localhost:9090/graph
   - Try queries like:
     - `up{job=~".*-service"}` - Service availability
     - `jvm_memory_used_bytes{area="heap"}` - Memory usage
     - `http_server_requests_seconds_count` - Request count

4. **Access Grafana dashboards:**
   - Login with admin/admin
   - Navigate to Dashboards
   - Select "RepoSync Microservices Overview"

### Using Kubernetes

1. **Deploy monitoring components:**
```bash
kubectl apply -f k8s/01-namespace-config.yaml
kubectl apply -f k8s/07-monitoring-service.yaml
kubectl apply -f k8s/07-prometheus.yaml
kubectl apply -f k8s/08-grafana.yaml
```

2. **Verify deployments:**
```bash
kubectl get pods -n reposync
kubectl get svc -n reposync
```

3. **Access services:**
```bash
# Port-forward Prometheus
kubectl port-forward -n reposync svc/prometheus 9090:9090

# Port-forward Grafana
kubectl port-forward -n reposync svc/grafana 3000:3000
```

## Monitoring Service API

The monitoring service provides REST endpoints:

### Endpoints

- `GET /api/monitoring/health` - System-wide health status
- `GET /api/monitoring/services/health` - Health of all services
- `GET /api/monitoring/services/{serviceName}/health` - Specific service health
- `GET /api/monitoring/services/unhealthy` - List of unhealthy services
- `POST /api/monitoring/health/check` - Trigger manual health check

### Example Usage

```bash
# Get system health
curl http://localhost:8085/api/monitoring/health

# Get all service health
curl http://localhost:8085/api/monitoring/services/health

# Get unhealthy services
curl http://localhost:8085/api/monitoring/services/unhealthy

# Trigger health check
curl -X POST http://localhost:8085/api/monitoring/health/check
```

## Prometheus Queries

### Useful PromQL Queries

**Service Availability:**
```promql
up{job=~".*-service"}
```

**Request Rate (per second):**
```promql
rate(http_server_requests_seconds_count[5m])
```

**95th Percentile Response Time:**
```promql
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (job, le))
```

**Memory Usage Percentage:**
```promql
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100
```

**Error Rate:**
```promql
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

**CPU Usage:**
```promql
system_cpu_usage * 100
```

## Grafana Dashboards

### RepoSync Overview Dashboard

The pre-configured dashboard includes:

1. **Service Availability Panel**: Real-time service status
2. **HTTP Request Rate**: Requests per second by service
3. **Response Time (95th percentile)**: Latency metrics
4. **JVM Memory Usage**: Heap memory consumption
5. **CPU Usage**: System and process CPU utilization
6. **Thread Count**: Active thread monitoring
7. **Error Rate**: 4xx and 5xx errors
8. **Garbage Collection Time**: GC performance

### Creating Custom Dashboards

1. Login to Grafana (http://localhost:3000)
2. Click "+" → "Dashboard"
3. Add panels with Prometheus queries
4. Save the dashboard

## Configuration

### Monitoring Service Configuration

Location: `monitoring-service/src/main/resources/application.yml`

Key configurations:
```yaml
monitoring:
  scrape-interval-seconds: 30
  health-check-timeout-ms: 5000
  services:
    - name: github-service
      url: http://github-service:8081
      enabled: true
```

### Prometheus Configuration

Location: `monitoring/prometheus/prometheus.yml`

Key configurations:
- Scrape interval: 15 seconds
- Evaluation interval: 15 seconds
- Retention period: Default (15 days)

### Alert Rules

Location: `monitoring/prometheus/rules/alerts.yml`

Customize alert thresholds and add new rules as needed.

## SOLID Principles Implementation

### Single Responsibility Principle (SRP)
- `HealthCheckService`: Only handles health checking
- `MetricsCollectionService`: Only handles metrics collection
- `MonitoringService`: Coordinates monitoring activities
- Each model class represents a single concept

### Open/Closed Principle (OCP)
- Services can be extended with new metric types without modification
- Easy to add new alert rules
- Dashboard templates can be customized

### Liskov Substitution Principle (LSP)
- Service implementations can be swapped without breaking functionality
- Interface-based design allows for alternative implementations

### Interface Segregation Principle (ISP)
- Focused interfaces: `HealthCheckService`, `MetricsCollectionService`
- Clients depend only on methods they use

### Dependency Inversion Principle (DIP)
- Services depend on abstractions (interfaces), not concrete implementations
- WebClient injected via constructor
- MeterRegistry abstraction for metrics

## Troubleshooting

### Services Not Showing Metrics

1. **Check service is running:**
```bash
docker ps | grep service-name
```

2. **Verify actuator endpoint:**
```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8081/actuator/prometheus
```

3. **Check Prometheus targets:**
   - Navigate to http://localhost:9090/targets
   - Ensure all services are "UP"

### Grafana Not Showing Data

1. **Verify Prometheus connection:**
   - Configuration → Data Sources → Prometheus
   - Click "Test" button

2. **Check time range:**
   - Ensure dashboard time range covers data collection period

3. **Verify Prometheus has data:**
   - Go to Prometheus UI
   - Run queries manually

### High Memory Usage Alerts

1. **Check JVM heap settings:**
```bash
# View container logs
docker logs <container-name>
```

2. **Adjust heap size in Dockerfile:**
```dockerfile
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]
```

## Performance Considerations

### Resource Requirements

- **Prometheus**: ~500MB RAM for 7 days retention
- **Grafana**: ~200MB RAM
- **Monitoring Service**: ~256MB RAM
- **Per Service Overhead**: ~50MB RAM for metrics

### Optimization Tips

1. **Reduce scrape frequency** if needed (15s → 30s)
2. **Limit metric cardinality** (avoid high-cardinality labels)
3. **Configure retention period** based on needs
4. **Use recording rules** for complex queries

## Security Considerations

1. **Change default Grafana password** in production
2. **Enable HTTPS** for external access
3. **Implement authentication** for Prometheus
4. **Use secrets** for sensitive configuration
5. **Network isolation** for monitoring components

## Maintenance

### Backup Prometheus Data
```bash
# Create backup
docker run --rm -v prometheus-data:/data -v $(pwd):/backup alpine tar czf /backup/prometheus-backup.tar.gz /data

# Restore backup
docker run --rm -v prometheus-data:/data -v $(pwd):/backup alpine tar xzf /backup/prometheus-backup.tar.gz -C /
```

### Update Dashboards
1. Export dashboard as JSON
2. Save to `monitoring/grafana/dashboards/`
3. Redeploy Grafana with new dashboards

## Further Reading

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

## Support

For issues or questions:
1. Check service logs: `docker logs <container-name>`
2. Review Prometheus alerts: http://localhost:9090/alerts
3. Examine metrics: http://localhost:9090/graph
4. Check monitoring service health: http://localhost:8085/api/monitoring/health

