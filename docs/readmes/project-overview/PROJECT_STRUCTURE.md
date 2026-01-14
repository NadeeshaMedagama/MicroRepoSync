# Project Structure

## Overview

This is a Spring Boot microservices application following SOLID principles with 5 independent services.

## Directory Structure

```
Microservices_with_RepoSync/
├── .github/
│   └── workflows/
│       ├── ci-cd.yml                    # CI/CD pipeline
│       └── daily-sync.yml               # Daily scheduled sync at 8 AM
├── common-lib/                          # Shared DTOs and utilities
│   ├── src/main/java/com/reposync/common/dto/
│   │   ├── RepositoryInfo.java
│   │   ├── DocumentContent.java
│   │   ├── TextChunk.java
│   │   ├── EmbeddingVector.java
│   │   └── SyncJobResult.java
│   └── pom.xml
├── github-service/                      # Port 8081
│   ├── src/main/java/com/reposync/github/
│   │   ├── GitHubServiceApplication.java
│   │   ├── config/GitHubConfig.java
│   │   ├── controller/GitHubController.java
│   │   └── service/GitHubService.java
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── Dockerfile
│   └── pom.xml
├── document-processor-service/          # Port 8082
│   ├── src/main/java/com/reposync/processor/
│   │   ├── DocumentProcessorApplication.java
│   │   ├── controller/DocumentProcessorController.java
│   │   └── service/ChunkingService.java
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── Dockerfile
│   └── pom.xml
├── embedding-service/                   # Port 8083
│   ├── src/main/java/com/reposync/embedding/
│   │   ├── EmbeddingServiceApplication.java
│   │   ├── config/AzureOpenAIConfig.java
│   │   ├── controller/EmbeddingController.java
│   │   └── service/AzureOpenAIService.java
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── Dockerfile
│   └── pom.xml
├── milvus-service/                      # Port 8084
│   ├── src/main/java/com/reposync/milvus/
│   │   ├── MilvusServiceApplication.java
│   │   ├── config/MilvusConfig.java
│   │   ├── controller/MilvusController.java
│   │   └── service/MilvusService.java
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── Dockerfile
│   └── pom.xml
├── orchestrator-service/                # Port 8080
│   ├── src/main/java/com/reposync/orchestrator/
│   │   ├── OrchestratorServiceApplication.java
│   │   ├── config/WebClientConfig.java
│   │   ├── controller/OrchestratorController.java
│   │   └── service/WorkflowOrchestrator.java
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── Dockerfile
│   └── pom.xml
├── k8s/                                 # Kubernetes manifests
│   ├── 01-namespace-config.yaml
│   ├── 02-github-service.yaml
│   ├── 03-document-processor-service.yaml
│   ├── 04-embedding-service.yaml
│   ├── 05-milvus-service.yaml
│   └── 06-orchestrator-service.yaml
├── .env.example                         # Environment variables template
├── .gitignore
├── docker-compose.yml                   # Local development
├── pom.xml                              # Parent POM
├── README.md                            # Full documentation
├── QUICKSTART.md                        # Quick start guide
├── start-services.sh                    # Helper script to start services
├── stop-services.sh                     # Helper script to stop services
└── deploy-k8s.sh                        # Kubernetes deployment script
```

## Service Dependencies

```
Parent POM (microservices-reposync)
├── common-lib
├── github-service (depends on common-lib)
├── document-processor-service (depends on common-lib)
├── embedding-service (depends on common-lib)
├── milvus-service (depends on common-lib)
└── orchestrator-service (depends on common-lib)
```

## Maven Modules

The project uses a multi-module Maven structure:

1. **Parent POM** - Manages versions and common dependencies
2. **common-lib** - Shared DTOs and utilities
3. **5 Microservices** - Each with its own build configuration

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.1 |
| Build Tool | Maven 3.9+ |
| Container | Docker |
| Orchestration | Kubernetes |
| GitHub API Client | WebClient (Spring WebFlux) |
| Azure OpenAI SDK | com.azure:azure-ai-openai |
| Vector Database | Milvus 2.3.4 |
| Scheduling | Spring @Scheduled |
| Resilience | Resilience4j |
| Monitoring | Spring Boot Actuator |

## Key Features by Service

### 1. GitHub Service
- Fetches repositories from organization
- Searches for README files (multiple patterns)
- Searches for API definitions (OpenAPI, Swagger)
- Filters by keyword
- REST API for repository data

### 2. Document Processor Service
- Recursive character text splitting
- Configurable chunk size and overlap
- Metadata preservation
- Batch processing support

### 3. Embedding Service
- Azure OpenAI integration
- Batch embedding generation
- Rate limit handling
- Automatic retry on failures

### 4. Milvus Service
- Collection management
- Vector upsert operations
- Schema creation with proper indexing
- JSON metadata support

### 5. Orchestrator Service
- Workflow coordination
- Scheduled sync (@Scheduled with cron)
- Manual trigger endpoint
- Retry logic with Resilience4j
- Comprehensive logging

## Environment Variables

### Required for All Environments
```
REPOSYNC_GITHUB_TOKEN
REPOSYNC_ORGANIZATION
AZURE_OPENAI_API_KEY
AZURE_OPENAI_ENDPOINT
AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT
MILVUS_URI
MILVUS_COLLECTION_NAME
```

### Optional
```
REPOSYNC_FILTER_KEYWORD
MILVUS_TOKEN
```

### Service URLs (auto-configured in Docker/K8s)
```
GITHUB_SERVICE_URL
PROCESSOR_SERVICE_URL
EMBEDDING_SERVICE_URL
MILVUS_SERVICE_URL
```

## Deployment Options

### 1. Local Development
- Individual Maven runs: `mvn spring-boot:run`
- Helper script: `./start-services.sh`
- Docker Compose: `docker-compose up`

### 2. GitHub Actions
- Daily sync at 8:00 AM UTC
- Manual trigger via workflow_dispatch
- Automated CI/CD on push

### 3. Kubernetes
- Full K8s manifests provided
- Deployment script: `./deploy-k8s.sh`
- Secrets and ConfigMaps for configuration

## Build Commands

```bash
# Build all modules
mvn clean install

# Build specific service
mvn clean install -pl github-service -am

# Skip tests
mvn clean install -DskipTests

# Build Docker images
docker-compose build

# Or individual service
docker build -t reposync/github-service:latest -f github-service/Dockerfile .
```

## Running Commands

```bash
# Local - Helper script
./start-services.sh

# Docker Compose
docker-compose up -d

# Kubernetes
./deploy-k8s.sh

# Manual trigger
curl -X POST http://localhost:8080/api/orchestrator/sync
```

## Testing

```bash
# Run all tests
mvn test

# Run specific service tests
cd github-service && mvn test

# Integration test (requires services running)
curl http://localhost:8080/actuator/health
```

## Monitoring

All services expose:
- `/actuator/health` - Health check
- `/actuator/info` - Service info
- `/actuator/metrics` - Metrics

## Logging

Logs available:
- Console output (local)
- Container logs: `docker-compose logs -f`
- Kubernetes logs: `kubectl logs -f deployment/SERVICE_NAME -n reposync`
- File logs: `logs/*.log` (individual run)

## SOLID Principles Implementation

- **Single Responsibility**: Each service has one clear purpose
- **Open/Closed**: Extensible via configuration, closed for modification
- **Liskov Substitution**: Services communicate via interfaces (REST APIs)
- **Interface Segregation**: Clean, focused REST endpoints
- **Dependency Inversion**: Services depend on abstractions (WebClient), not implementations

## Workflow Execution

1. **Scheduled Trigger** (8:00 AM daily) or Manual API call
2. **Orchestrator** → Calls GitHub Service
3. **GitHub Service** → Returns repositories and documents
4. **Orchestrator** → Calls Document Processor
5. **Document Processor** → Returns chunks
6. **Orchestrator** → Calls Embedding Service
7. **Embedding Service** → Returns vectors
8. **Orchestrator** → Calls Milvus Service
9. **Milvus Service** → Stores vectors
10. **Orchestrator** → Returns SyncJobResult

## Next Steps

1. Configure `.env` with your credentials
2. Run `./start-services.sh` for local testing
3. Set up GitHub Actions secrets
4. Deploy to Kubernetes for production
5. Monitor logs and metrics
6. Customize as needed

## Support

- Full documentation: [README.md](../../README.md)
- Quick start: [QUICKSTART.md](QUICKSTART.md)
- Issues: GitHub Issues

