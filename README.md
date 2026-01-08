# RepoSync Microservices

A comprehensive Spring Boot microservices application that automatically syncs GitHub organization repositories to Milvus vector database. The system fetches README files and API definition files, chunks them, generates embeddings using Azure OpenAI, and stores them in Milvus for vector search capabilities.

## 🏗️ Architecture

This project follows a microservices architecture with five independent services:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Orchestrator Service                          │
│           (Coordinates workflow, Scheduled jobs)                 │
└────┬────────────┬────────────┬────────────┬─────────────────────┘
     │            │            │            │
     ▼            ▼            ▼            ▼
┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐
│ GitHub  │  │Document │  │Embedding│  │ Milvus  │
│ Service │  │Processor│  │ Service │  │ Service │
│         │  │ Service │  │         │  │         │
└─────────┘  └─────────┘  └─────────┘  └─────────┘
     │            │            │            │
     ▼            ▼            ▼            ▼
┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐
│ GitHub  │  │Chunking │  │ Azure   │  │ Milvus  │
│   API   │  │ Logic   │  │ OpenAI  │  │  DB     │
└─────────┘  └─────────┘  └─────────┘  └─────────┘
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
   - Handles vector upserts and schema management
   - Automatic collection creation with proper indexing

5. **Orchestrator Service** (Port 8080)
   - Coordinates the entire sync workflow
   - Scheduled execution (daily at 8:00 AM)
   - Manual trigger via REST API
   - Resilience with retry logic

## 🚀 Features

- ✅ **Automated Daily Sync**: Runs at 8:00 AM every day via GitHub Actions
- ✅ **Microservices Architecture**: Independent, scalable services following SOLID principles
- ✅ **Docker & Kubernetes Ready**: Complete containerization and K8s manifests
- ✅ **CI/CD Pipeline**: Automated build, test, and deployment
- ✅ **Local & Cloud Support**: Works in both local and GitHub Actions environments
- ✅ **Resilience**: Retry logic, circuit breakers, and fallback mechanisms
- ✅ **Observability**: Health checks, metrics, and logging

## 📋 Prerequisites

- Java 21 (OpenJDK 21 or higher)
- Maven 3.6+
- Docker and Docker Compose (optional but recommended)
- Kubernetes cluster (for production deployment)
- GitHub Personal Access Token
- Azure OpenAI API access
- Milvus instance (cloud or self-hosted)

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
REPOSYNC_GITHUB_TOKEN=ghp_your_token_here
REPOSYNC_ORGANIZATION=your-org-name
REPOSYNC_FILTER_KEYWORD=microservices
AZURE_OPENAI_API_KEY=your-azure-openai-key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT=text-embedding-ada-002
MILVUS_URI=localhost:19530
MILVUS_TOKEN=
MILVUS_COLLECTION_NAME=reposync_collection
```

### 3. Build the Project

```bash
mvn clean install
```

## 🏃 Running Locally

### Option 1: Using Docker Compose (Recommended)

```bash
# Start all services including Milvus
docker-compose up -d

# Check logs
docker-compose logs -f

# Trigger manual sync
curl -X POST http://localhost:8080/api/orchestrator/sync

# Stop all services
docker-compose down
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

| Service | Port |
|---------|------|
| Orchestrator | 8080 |
| GitHub | 8081 |
| Document Processor | 8082 |
| Embedding | 8083 |
| Milvus | 8084 |

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
  schedule:
    cron: "0 0 8 * * *"  # Daily at 8:00 AM
```

## 📊 Monitoring

### Health Checks

Each service exposes actuator endpoints:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

### Metrics

```bash
curl http://localhost:8080/actuator/metrics
```

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

## 📄 License

This project is licensed under the MIT License.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📧 Support

For issues and questions, please open an issue on GitHub.

