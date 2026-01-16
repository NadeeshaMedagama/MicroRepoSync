# RepoSync Microservices

A comprehensive Spring Boot microservices application that automatically syncs GitHub organization repositories to Milvus vector database. The system fetches README files and API definition files, chunks them, generates embeddings using Azure OpenAI, and stores them in Milvus for vector search capabilities.

## 🏗️ Architecture

This project follows a microservices architecture with six independent services plus a complete monitoring stack:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Orchestrator Service                          │
│           (Coordinates workflow, Scheduled jobs)                 │
└────┬────────────┬────────────┬────────────┬─────────────────────┘
     │            │            │            │
     ▼            ▼            ▼            ▼
┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌──────────┐
│ GitHub  │  │Document │  │Embedding│  │ Milvus  │  │Monitoring│
│ Service │  │Processor│  │ Service │  │ Service │  │ Service  │
│         │  │ Service │  │         │  │         │  │          │
└─────────┘  └─────────┘  └─────────┘  └─────────┘  └────┬─────┘
     │            │            │            │              │
     ▼            ▼            ▼            ▼              ▼
┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌──────────┐
│ GitHub  │  │Chunking │  │ Azure   │  │ Milvus  │  │Prometheus│
│   API   │  │ Logic   │  │ OpenAI  │  │  DB     │  │ & Grafana│
└─────────┘  └─────────┘  └─────────┘  └─────────┘  └──────────┘
```

### Services

1. **GitHub Service** (Port 8081)
   - Fetches repositories from GitHub organization
   - Retrieves README and API definition files
   - Filters repositories by keyword

2. **Document Processor Service** (Port 8082)
   - Chunks documents using recursive character splitting
   - Maintains context with configurable overlap
   - Preserves metadata for each chunk

3. **Embedding Service** (Port 8083)
   - Generates embeddings using Azure OpenAI
   - Batch processing with rate limit handling
   - Automatic retry on failures

4. **Milvus Service** (Port 8084)
   - Manages Milvus vector database collections
   - **Automatic collection creation** if it doesn't exist
   - Handles vector upserts and schema management
   - Works with cloud Milvus (Zilliz)

5. **Orchestrator Service** (Port 8080)
   - Coordinates the entire sync workflow
   - **Auto-sync on startup** (configurable, enabled by default)
   - Scheduled execution (daily at 8:00 AM)
   - Manual trigger via REST API
   - Resilience with retry logic

6. **Monitoring Service** (Port 8085)
   - Health monitoring and metrics aggregation
   - Automated health checks every 30 seconds
   - REST API for monitoring status
   - Exposes metrics to Prometheus

### Monitoring Stack

- **Prometheus** (Port 9090)
  - Metrics collection and storage
  - Scrapes all services every 15 seconds
  - Time-series database
  - Alert rule evaluation

- **Grafana** (Port 3000)
  - Real-time dashboards and visualization
  - Pre-configured dashboards for all services
  - Connected to Prometheus
  - Default credentials: admin/admin

## 🚀 Features

- ✅ **Auto-Sync on Startup**: Automatically fetches and syncs repositories when application starts
- ✅ **Automatic Collection Creation**: Milvus collection created automatically if it doesn't exist
- ✅ **Automated Daily Sync**: Runs at 8:00 AM every day via scheduled task and GitHub Actions
- ✅ **Cloud-Native Vector Storage**: Uses Zilliz cloud Milvus (no local database required)
- ✅ **Microservices Architecture**: Independent, scalable services following SOLID principles
- ✅ **Complete Monitoring System**: Prometheus + Grafana with custom monitoring service
- ✅ **Real-time Dashboards**: 8 pre-configured Grafana panels for all metrics
- ✅ **Intelligent Alerting**: 8 alert rules for critical conditions
- ✅ **Docker & Kubernetes Ready**: Complete containerization and K8s manifests
- ✅ **CI/CD Pipeline**: Automated build, test, and deployment
- ✅ **Dependency Updates**: Weekly automated dependency and security checks
- ✅ **Security Scanning**: OWASP, Trivy, and license compliance checks
- ✅ **Local & Cloud Support**: Works in both local and GitHub Actions environments
- ✅ **Resilience**: Retry logic, circuit breakers, and fallback mechanisms
- ✅ **Full Observability**: Health checks, metrics, logging, and monitoring

## 📋 Prerequisites

- Java 21 (OpenJDK 21 or higher)
- Maven 3.6+
- Docker and Docker Compose
- GitHub Personal Access Token
- Azure OpenAI API access
- **Cloud Milvus instance** (Zilliz Cloud recommended)

## 🛠️ Setup

### 1. Clone the Repository

```bash
git clone <your-repo-url>
cd Microservices_with_RepoSync
```

### 2. Configure Environment Variables

Copy the example environment file:

```bash
cp .env.example .env
```

Edit `.env` and fill in your credentials:

```env
# GitHub Configuration
REPOSYNC_GITHUB_TOKEN=ghp_your_token_here
REPOSYNC_ORGANIZATION=your-org-name
REPOSYNC_FILTER_KEYWORD=microservices

# Azure OpenAI Configuration
AZURE_OPENAI_API_KEY=your-azure-openai-key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT=text-embedding-ada-002

# Cloud Milvus (Zilliz) Configuration
MILVUS_URI=https://your-instance.vectordb.xxxxxxxxxx.com:19530
MILVUS_TOKEN=your-zilliz-token
MILVUS_COLLECTION_NAME=reposync_collection

# Auto-Sync Configuration (Optional)
REPOSYNC_AUTO_SYNC_ON_STARTUP=true  # Default: true
```

### 3. Build the Project

```bash
# Full build with all checks
mvn clean install

# Fast build (skip tests and checkstyle)
mvn clean package -DskipTests -Dcheckstyle.skip=true
```

## 🏃 Running Locally

### Quick Start with Auto-Sync (Recommended)

The easiest way to run the application is using the start script, which handles everything automatically:

```bash
# Run this single command - everything is automatic!
./scripts/start-local.sh

# The script will:
# 1. Check prerequisites (Java 21, Maven, Docker)
# 2. Validate your .env configuration
# 3. Build all services
# 4. Start Docker Compose
# 5. Wait for services to be healthy
# 6. Auto-sync triggers automatically after 5 seconds!

# Verify auto-sync is working
./scripts/verify-auto-sync.sh

# Watch live sync progress
docker compose logs -f orchestrator-service
```

**What happens automatically:**
- ✅ Fetches all repositories from your GitHub organization
- ✅ Extracts documents from each repository
- ✅ Chunks documents for optimal embedding
- ✅ Generates embeddings using Azure OpenAI
- ✅ **Creates Milvus collection if it doesn't exist**
- ✅ Stores all vectors in your cloud Milvus collection

### Option 1: Using Docker Compose

```bash
# Start all services including monitoring stack
docker compose up -d

# Auto-sync will trigger automatically in 5 seconds after startup!

# Check logs
docker compose logs -f

# (Optional) Manually trigger sync if needed
curl -X POST http://localhost:8080/api/orchestrator/sync

# Access monitoring interfaces
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
# Monitoring API: http://localhost:8085/api/monitoring

# Stop all services
docker compose down
```

### Option 2: Running Individual Services

Open 5 terminal windows and run:

```bash
# Terminal 1 - GitHub Service
cd github-service
mvn spring-boot:run

# Terminal 2 - Document Processor Service
cd document-processor-service
mvn spring-boot:run

# Terminal 3 - Embedding Service
cd embedding-service
mvn spring-boot:run

# Terminal 4 - Milvus Service
cd milvus-service
mvn spring-boot:run

# Terminal 5 - Orchestrator Service
cd orchestrator-service
mvn spring-boot:run

# Note: Auto-sync will trigger when orchestrator starts!
```

### Disabling Auto-Sync (Optional)

If you prefer to trigger sync manually:

```bash
# Set environment variable before starting
export REPOSYNC_AUTO_SYNC_ON_STARTUP=false

# Then start services
./scripts/start-local.sh

# Or with Docker Compose
docker compose restart orchestrator-service

# Manually trigger sync when needed
curl -X POST http://localhost:8080/api/orchestrator/sync | jq '.'
```

## ☁️ Deploying to Kubernetes

### 1. Update Kubernetes Secrets

Edit `k8s/01-namespace-config.yaml` and add your credentials to the Secret.

### 2. Deploy to Cluster

```bash
# Apply all manifests
kubectl apply -f k8s/

# Check deployment status
kubectl get pods -n reposync
kubectl get services -n reposync

# View logs
kubectl logs -f deployment/orchestrator-service -n reposync
```

### 3. Trigger Manual Sync

```bash
# Get the orchestrator service external IP
kubectl get service orchestrator-service -n reposync

# Trigger sync
curl -X POST http://<EXTERNAL-IP>:8080/api/orchestrator/sync
```

## 🤖 GitHub Actions Setup

### 1. Configure Secrets

Go to your GitHub repository → Settings → Secrets and variables → Actions

Add the following secrets:

- `REPOSYNC_GITHUB_TOKEN`
- `REPOSYNC_ORGANIZATION`
- `REPOSYNC_FILTER_KEYWORD`
- `AZURE_OPENAI_API_KEY`
- `AZURE_OPENAI_ENDPOINT`
- `AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT`
- `MILVUS_URI`
- `MILVUS_TOKEN`
- `MILVUS_COLLECTION_NAME`
- `DOCKER_USERNAME` (for CI/CD)
- `DOCKER_PASSWORD` (for CI/CD)
- `KUBE_CONFIG` (base64 encoded kubeconfig for CI/CD)

### 2. Workflows

Two workflows are configured:

1. **Daily Sync** (`.github/workflows/daily-sync.yml`)
   - Runs daily at 8:00 AM UTC
   - Can be triggered manually
   - Executes the complete sync workflow

2. **CI/CD Pipeline** (`.github/workflows/ci-cd.yml`)
   - Runs on push to main/develop
   - Builds and tests all services
   - Builds and pushes Docker images
   - Deploys to Kubernetes

## 🔧 Configuration

### Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Orchestrator | 8080 | Main workflow coordinator |
| GitHub | 8081 | GitHub API integration |
| Document Processor | 8082 | Document chunking |
| Embedding | 8083 | Azure OpenAI embeddings |
| Milvus | 8084 | Vector database service |
| Monitoring | 8085 | Health & metrics aggregation |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3000 | Monitoring dashboards |

### Chunking Configuration

Edit `document-processor-service/src/main/resources/application.yml`:

```yaml
chunking:
  chunk-size: 1000      # Characters per chunk
  overlap: 200          # Overlap between chunks
```

### Schedule Configuration

Edit `orchestrator-service/src/main/resources/application.yml`:

```yaml
reposync:
  auto-sync-on-startup: true  # Enable/disable auto-sync on startup
  schedule:
    cron: "0 0 8 * * *"  # Daily at 8:00 AM
```

Or use environment variable:

```bash
export REPOSYNC_AUTO_SYNC_ON_STARTUP=false  # Disable auto-sync
```

## 📊 Monitoring & Observability

The application includes a comprehensive monitoring system built with **Prometheus** and **Grafana**, following SOLID principles.

### Quick Start Monitoring

```bash
# Start the monitoring stack
./docs/scripts/start-monitoring.sh

# Or manually with docker-compose
docker-compose up -d monitoring-service prometheus grafana
```

### Access Monitoring Interfaces

| Interface | URL | Credentials | Description |
|-----------|-----|-------------|-------------|
| **Grafana** | http://localhost:3000 | admin/admin | Visual dashboards |
| **Prometheus** | http://localhost:9090 | - | Metrics & queries |
| **Monitoring API** | http://localhost:8085/api/monitoring | - | Health status API |

### Monitoring Service Features

The **Monitoring Service** (Port 8085) provides:

- **Automated Health Checks**: Polls all services every 30 seconds
- **Metrics Aggregation**: Collects and aggregates metrics from all services
- **REST API**: Programmatic access to health and metrics data
- **Prometheus Integration**: Exposes metrics in Prometheus format

#### Monitoring API Endpoints

```bash
# Get system-wide health status
curl http://localhost:8085/api/monitoring/health

# Get health of all services
curl http://localhost:8085/api/monitoring/services/health

# Get specific service health
curl http://localhost:8085/api/monitoring/services/github-service/health

# Get unhealthy services
curl http://localhost:8085/api/monitoring/services/unhealthy

# Trigger manual health check
curl -X POST http://localhost:8085/api/monitoring/health/check
```

### Prometheus Metrics

**Prometheus** (Port 9090) collects metrics from all services:

- **Scrape Interval**: 15 seconds
- **Retention**: 15 days (default)
- **Alert Evaluation**: Every 15 seconds

#### Available Metrics

Each service exposes Prometheus metrics at `/actuator/prometheus`:

- **JVM Metrics**: Memory, threads, GC, class loading
- **HTTP Metrics**: Request count, latency, status codes
- **System Metrics**: CPU, disk, uptime
- **Custom Metrics**: Service-specific business metrics

#### Useful Prometheus Queries

```promql
# Service availability
up{job=~".*-service"}

# Request rate (requests per second)
rate(http_server_requests_seconds_count[5m])

# Memory usage percentage
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# 95th percentile response time
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (job, le))

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# CPU usage
system_cpu_usage * 100
```

### Grafana Dashboards

**Grafana** (Port 3000) provides real-time visualization:

- **Pre-configured Dashboard**: RepoSync Microservices Overview
- **8 Monitoring Panels**:
  1. Service Availability - Real-time service status
  2. HTTP Request Rate - Requests per second by service
  3. Response Time (95th percentile) - Latency tracking
  4. JVM Memory Usage - Heap memory monitoring
  5. CPU Usage - System and process CPU
  6. Thread Count - Thread pool monitoring
  7. Error Rate - 4xx and 5xx errors
  8. Garbage Collection Time - GC performance

#### Accessing Grafana

1. Navigate to http://localhost:3000
2. Login with `admin` / `admin`
3. Dashboard is auto-provisioned and ready to use

### Alert Rules

The system includes **8 pre-configured alert rules**:

| Alert | Condition | Severity |
|-------|-----------|----------|
| **ServiceDown** | Service down > 1 minute | Critical |
| **HighMemoryUsage** | Heap > 85% for 5 minutes | Warning |
| **CriticalMemoryUsage** | Heap > 95% for 2 minutes | Critical |
| **HighCPUUsage** | CPU > 80% for 5 minutes | Warning |
| **HighErrorRate** | Error rate > 10% | Critical |
| **LowRequestRate** | Request rate very low | Info |
| **FrequentGC** | GC > 5 times/sec for 5 min | Warning |
| **HighThreadCount** | Threads > 200 | Warning |

View active alerts in Prometheus: http://localhost:9090/alerts

### Health Checks

Each service exposes Spring Boot Actuator health endpoints:

```bash
# Check individual services
curl http://localhost:8080/actuator/health  # Orchestrator
curl http://localhost:8081/actuator/health  # GitHub
curl http://localhost:8082/actuator/health  # Document Processor
curl http://localhost:8083/actuator/health  # Embedding
curl http://localhost:8084/actuator/health  # Milvus
curl http://localhost:8085/actuator/health  # Monitoring
```

### Metrics Endpoints

Access Prometheus-formatted metrics:

```bash
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8081/actuator/prometheus
# ... etc for all services
```

### Monitoring Documentation

For detailed monitoring documentation, see:

- **[Monitoring Guide](docs/readmes/monitoring/MONITORING_GUIDE.md)** - Comprehensive 400+ line guide
- **[Monitoring Quick Start](docs/readmes/monitoring/MONITORING_QUICKSTART.md)** - Quick reference
- **[Monitoring Architecture](docs/readmes/monitoring/MONITORING_ARCHITECTURE.md)** - Architecture diagrams
- **[Monitoring Implementation](docs/readmes/monitoring/MONITORING_IMPLEMENTATION_SUMMARY.md)** - Implementation details

### Troubleshooting Monitoring

**Services not showing in Prometheus:**
1. Check Prometheus targets: http://localhost:9090/targets
2. Verify services are running: `docker ps`
3. Check actuator endpoints: `curl http://localhost:8081/actuator/prometheus`

**Grafana shows no data:**
1. Verify Prometheus connection in Configuration → Data Sources
2. Check time range in dashboard
3. Run queries in Prometheus UI first

**High memory alerts:**
1. Check service logs: `docker logs <container-name>`
2. Review JVM heap settings in Dockerfile
3. Consider increasing memory allocation

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run tests for specific service
cd github-service
mvn test
```

## 📝 API Documentation

### Orchestrator Service

- `POST /api/orchestrator/sync` - Trigger manual sync
- `GET /api/orchestrator/health` - Health check

### GitHub Service

- `GET /api/github/repositories?organization={org}&filterKeyword={keyword}` - Get repositories
- `GET /api/github/documents/{owner}/{repo}` - Get documents from repository

### Document Processor Service

- `POST /api/processor/chunk` - Chunk single document
- `POST /api/processor/chunk/batch` - Chunk multiple documents

### Embedding Service

- `POST /api/embedding/generate` - Generate embedding for single chunk
- `POST /api/embedding/generate/batch` - Generate embeddings for multiple chunks

### Milvus Service

- `POST /api/milvus/collection/create` - Create collection
- `POST /api/milvus/vectors/upsert` - Upsert vectors
- `GET /api/milvus/collection/{name}/exists` - Check collection existence

### Monitoring Service

- `GET /api/monitoring/health` - Get system-wide health status
- `GET /api/monitoring/services/health` - Get health of all services
- `GET /api/monitoring/services/{serviceName}/health` - Get specific service health
- `GET /api/monitoring/services/unhealthy` - Get list of unhealthy services
- `POST /api/monitoring/health/check` - Trigger manual health check

## 🛡️ SOLID Principles Implementation

- **Single Responsibility**: Each service has one clear responsibility
- **Open/Closed**: Services can be extended without modification
- **Liskov Substitution**: Services can be replaced with compatible implementations
- **Interface Segregation**: Clean REST APIs with specific endpoints
- **Dependency Inversion**: Services depend on abstractions (REST APIs), not implementations

## 🔄 Workflow

1. **Fetch Repositories**: GitHub Service retrieves repositories matching criteria
2. **Extract Documents**: README and API definition files are extracted
3. **Chunk Documents**: Documents are split into manageable chunks with overlap
4. **Generate Embeddings**: Azure OpenAI creates vector embeddings
5. **Store in Milvus**: Vectors with metadata are stored in Milvus collection

## ⚡ Auto-Sync Feature

### How It Works

When you run `./scripts/start-local.sh`, the system automatically:

1. **Starts All Services** - Docker Compose brings up all microservices
2. **Waits for Health** - Ensures all services are ready (health checks pass)
3. **Triggers Sync** - After 5 seconds, the orchestrator automatically initiates sync
4. **Fetches Data** - Retrieves all repositories from your GitHub organization
5. **Processes Documents** - Extracts, chunks, and embeds all documents
6. **Creates Collection** - If the Milvus collection doesn't exist, creates it automatically
7. **Stores Vectors** - Upserts all embeddings to your cloud Milvus collection

### Quick Commands

```bash
# Start with auto-sync (default)
./scripts/start-local.sh

# Verify auto-sync status
./scripts/verify-auto-sync.sh

# Watch sync progress
docker compose logs -f orchestrator-service

# Disable auto-sync
export REPOSYNC_AUTO_SYNC_ON_STARTUP=false
docker compose restart orchestrator-service

# Manually trigger sync
curl -X POST http://localhost:8080/api/orchestrator/sync | jq '.'
```

### Benefits

- ✅ **Zero Manual Steps** - Just run one script
- ✅ **Immediate Data** - Your vector database is populated on first startup
- ✅ **Auto-Recovery** - Collection created if missing
- ✅ **Scheduled Updates** - Daily sync at 8:00 AM keeps data fresh
- ✅ **Cloud-Native** - Works with Zilliz cloud Milvus

For detailed documentation, see [AUTO_SYNC_IMPLEMENTATION.md](AUTO_SYNC_IMPLEMENTATION.md).

## 🐛 Troubleshooting

### Service won't start

```bash
# Check if port is already in use
lsof -i :8080

# Check logs
docker-compose logs <service-name>
```

### Connection refused between services

- Ensure all services are running
- Check service URLs in configuration
- Verify network connectivity in Docker/K8s

### Milvus connection issues

- Verify MILVUS_URI is correct
- Check if Milvus is running: `docker ps | grep milvus`
- Review Milvus logs: `docker logs milvus-standalone`

## 📚 Documentation

Comprehensive documentation is available in the `docs/readmes/` directory:

### 🎯 Quick Start & Auto-Sync
- **[Auto-Sync Implementation](AUTO_SYNC_IMPLEMENTATION.md)** - Complete auto-sync guide
- **[Implementation Summary](IMPLEMENTATION_SUMMARY.md)** - Technical implementation details
- **[Quick Start Guide](docs/readmes/setup-guides/QUICKSTART.md)** - Get started in 5 minutes
- **[Local Setup Guide](docs/readmes/setup-guides/LOCAL_SETUP_GUIDE.md)** - Detailed local development setup

### 📊 Monitoring & Observability
- **[Monitoring Guide](docs/readmes/monitoring/MONITORING_GUIDE.md)** - Comprehensive monitoring guide (400+ lines)
- **[Monitoring Quick Start](docs/readmes/monitoring/MONITORING_QUICKSTART.md)** - Quick reference
- **[Monitoring Architecture](docs/readmes/monitoring/MONITORING_ARCHITECTURE.md)** - Architecture diagrams
- **[Monitoring Implementation](docs/readmes/monitoring/MONITORING_IMPLEMENTATION_SUMMARY.md)** - Implementation details

### 🚀 Setup & Configuration Guides
- **[Local Run Guide](docs/readmes/setup-guides/LOCAL_RUN_GUIDE.md)** - Running services locally
- **[IntelliJ IDEA Guide](docs/readmes/setup-guides/INTELLIJ_GUIDE.md)** - IDE setup and configuration
- **[Setup Checklist](docs/readmes/setup-guides/SETUP_CHECKLIST.md)** - Complete setup checklist
- **[Visual Guide](docs/readmes/setup-guides/VISUAL_GUIDE.md)** - Screenshots and visual walkthrough

### 🏗️ Architecture & Design
- **[Project Structure](docs/readmes/project-overview/PROJECT_STRUCTURE.md)** - Project organization and structure
- **[Pipeline Architecture](docs/readmes/ci-cd/PIPELINE_ARCHITECTURE.md)** - CI/CD pipeline architecture

### 🔄 CI/CD & Automation
- **[GitHub Actions Pipeline](.github/GITHUB_ACTIONS_PIPELINE.md)** - Complete pipeline documentation
- **[Dependency Updates Pipeline](docs/readmes/ci-cd/DEPENDENCY_UPDATES_PIPELINE.md)** - Security and dependency management
- **[Dependency Updates Quick Start](docs/readmes/ci-cd/DEPENDENCY_UPDATES_QUICKSTART.md)** - Quick reference guide
- **[Integration Verification](docs/readmes/integration/INTEGRATION_VERIFICATION.md)** - Pipeline integration validation
- **[Implementation Summary](docs/readmes/ci-cd/COMPLETE_WORKFLOWS_IMPLEMENTATION.md)** - Complete implementation guide

### 🔨 Build & Deployment
- **[Build Status](docs/readmes/build-deployment/BUILD_STATUS.md)** - Current build status
- **[Java 21 Build Fix](docs/readmes/build-deployment/JAVA21_BUILD_FIX.md)** - Java 21 migration notes
- **[Build Fix Summary](docs/readmes/build-deployment/BUILD_FIX_SUMMARY.md)** - Build fixes applied

### 📋 Project Status
- **[Project Complete](docs/readmes/project-overview/PROJECT_COMPLETE.md)** - Project completion summary
- **[Final Summary](docs/readmes/project-overview/FINAL_SUMMARY.md)** - Final project summary

### 📖 Main Documentation Index
- **[Complete Documentation Index](docs/readmes/README.md)** - Navigate all documentation

## 🔒 Security

### Automated Security Scanning

The project includes comprehensive security scanning:

- **OWASP Dependency Check**: Weekly CVE scanning (every Monday 9 AM UTC)
- **Trivy Scanner**: Multi-purpose vulnerability scanning
- **License Compliance**: Automated third-party license tracking
- **GitHub Security**: Integration with GitHub Advanced Security

### Security Reports

Security reports are generated automatically and available in GitHub Actions artifacts:
- OWASP Dependency Check Reports (HTML)
- Trivy Security Reports (SARIF)
- Dependency Tree Analysis
- License Compliance Reports

See the [Dependency Updates Pipeline Documentation](docs/readmes/DEPENDENCY_UPDATES_PIPELINE.md) for details.

## 📄 License

This project is licensed under the MIT License.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📧 Support

For issues and questions, please open an issue on GitHub.

