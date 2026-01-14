# Quick Start Guide - Running Locally Without Docker

This guide shows you how to run the RepoSync microservices locally on your machine without Docker, which is useful for development and debugging.

## Prerequisites

✅ **Java 21** - Verified installed (OpenJDK 21.0.9)
✅ **Maven** - Required for building and running services
✅ **Milvus** - Vector database (can run in Docker even if services run locally)

## Step-by-Step Instructions

### 1. Configure Environment Variables

Edit the `.env` file with your actual credentials:

```bash
nano .env
```

**Required Configuration:**
```bash
# GitHub Configuration
REPOSYNC_GITHUB_TOKEN=ghp_your_actual_token_here
REPOSYNC_ORGANIZATION=your-actual-org-name
REPOSYNC_FILTER_KEYWORD=java  # or leave empty

# Azure OpenAI Configuration
AZURE_OPENAI_API_KEY=your-actual-azure-key
AZURE_OPENAI_ENDPOINT=https://your-actual-resource.openai.azure.com/
AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT=text-embedding-ada-002

# Milvus Configuration
MILVUS_URI=localhost:19530
MILVUS_TOKEN=  # leave empty for local Milvus
MILVUS_COLLECTION_NAME=reposync_collection

# Service URLs (for local development)
GITHUB_SERVICE_URL=http://localhost:8081
PROCESSOR_SERVICE_URL=http://localhost:8082
EMBEDDING_SERVICE_URL=http://localhost:8083
MILVUS_SERVICE_URL=http://localhost:8084
```

### 2. Start Milvus Vector Database (Using Docker)

Even when running services locally, you need Milvus for vector storage:

```bash
# Start only Milvus and its dependencies
docker-compose up -d milvus-standalone milvus-etcd milvus-minio

# Verify Milvus is running
docker-compose ps

# Check Milvus health
curl http://localhost:19530/healthz
```

Expected output:
```
NAME                  IMAGE                    STATUS
milvus-standalone     milvusdb/milvus:v2.3.4  Up
milvus-etcd           quay.io/coreos/etcd     Up
milvus-minio          minio/minio             Up
```

### 3. Load Environment Variables

Export the environment variables to your shell:

```bash
# Export all variables from .env
export $(cat .env | grep -v '^#' | xargs)
```

Or create a simple script `load-env.sh`:
```bash
#!/bin/bash
set -a
source .env
set +a
```

```bash
chmod +x load-env.sh
source ./load-env.sh
```

### 4. Start Services (Each in a Separate Terminal)

Open **5 terminal windows** and run one service in each:

#### Terminal 1: GitHub Service
```bash
cd /home/nadeeshame/IdeaProjects/Microservices_with_RepoSync/github-service
export $(cat ../.env | grep -v '^#' | xargs)
mvn spring-boot:run
```

Wait for: `Started GitHubServiceApplication in X seconds`

#### Terminal 2: Document Processor Service
```bash
cd /home/nadeeshame/IdeaProjects/Microservices_with_RepoSync/document-processor-service
export $(cat ../.env | grep -v '^#' | xargs)
mvn spring-boot:run
```

Wait for: `Started DocumentProcessorApplication in X seconds`

#### Terminal 3: Embedding Service
```bash
cd /home/nadeeshame/IdeaProjects/Microservices_with_RepoSync/embedding-service
export $(cat ../.env | grep -v '^#' | xargs)
mvn spring-boot:run
```

Wait for: `Started EmbeddingServiceApplication in X seconds`

#### Terminal 4: Milvus Service
```bash
cd /home/nadeeshame/IdeaProjects/Microservices_with_RepoSync/milvus-service
export $(cat ../.env | grep -v '^#' | xargs)
mvn spring-boot:run
```

Wait for: `Started MilvusServiceApplication in X seconds`

#### Terminal 5: Orchestrator Service
```bash
cd /home/nadeeshame/IdeaProjects/Microservices_with_RepoSync/orchestrator-service
export $(cat ../.env | grep -v '^#' | xargs)
mvn spring-boot:run
```

Wait for: `Started OrchestratorApplication in X seconds`

### 5. Verify All Services Are Running

In a new terminal, check each service health:

```bash
# GitHub Service
curl http://localhost:8081/actuator/health
# Expected: {"status":"UP"}

# Document Processor Service
curl http://localhost:8082/actuator/health
# Expected: {"status":"UP"}

# Embedding Service
curl http://localhost:8083/actuator/health
# Expected: {"status":"UP"}

# Milvus Service
curl http://localhost:8084/actuator/health
# Expected: {"status":"UP"}

# Orchestrator Service
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### 6. Trigger a Manual Sync

Once all services are running, trigger a sync job:

```bash
curl -X POST http://localhost:8080/api/sync/trigger \
  -H "Content-Type: application/json" \
  -v
```

Expected response:
```json
{
  "message": "Sync job triggered successfully",
  "jobId": "...",
  "status": "STARTED"
}
```

### 7. Monitor the Sync Process

Watch the orchestrator service logs (Terminal 5) to see the sync progress:

```
INFO  c.r.o.service.SyncOrchestrator - Starting sync for organization: your-org
INFO  c.r.o.service.SyncOrchestrator - Found 10 repositories
INFO  c.r.o.service.SyncOrchestrator - Processing repository: repo-name
INFO  c.r.o.service.SyncOrchestrator - Found 3 documents
INFO  c.r.o.service.SyncOrchestrator - Generated 45 chunks
INFO  c.r.o.service.SyncOrchestrator - Created 45 embeddings
INFO  c.r.o.service.SyncOrchestrator - Stored vectors in Milvus
INFO  c.r.o.service.SyncOrchestrator - Sync completed successfully
```

### 8. Query the Sync Status

Check the last sync job result:

```bash
curl http://localhost:8080/api/sync/status
```

## Alternative: Using IntelliJ IDEA

Instead of running from terminal, you can run each service from IntelliJ:

1. **Open the project** in IntelliJ IDEA
2. **Configure environment variables** in Run Configurations:
   - Go to: Run → Edit Configurations
   - Select or create a Spring Boot configuration for each service
   - Add environment variables from `.env` file
3. **Run each service** using the green play button
4. **Use the Services tool window** to manage all running services

### IntelliJ Run Configuration Example:

For each service, create a Spring Boot run configuration:

**Name:** `OrchestratorService`
**Main class:** `com.reposync.orchestrator.OrchestratorApplication`
**Environment variables:**
```
REPOSYNC_ORGANIZATION=your-org;REPOSYNC_FILTER_KEYWORD=java;GITHUB_SERVICE_URL=http://localhost:8081;...
```

## Troubleshooting

### Port Already in Use

If you get "Port already in use" error:

```bash
# Find and kill process using the port
lsof -i :8080
kill -9 <PID>
```

### Service Cannot Connect to Another Service

1. Check the service URL in environment variables
2. Verify the target service is running
3. Check firewall settings

### Milvus Connection Failed

```bash
# Check if Milvus is running
docker-compose ps milvus-standalone

# Restart Milvus if needed
docker-compose restart milvus-standalone

# Check Milvus logs
docker-compose logs milvus-standalone
```

### Out of Memory Error

Increase Maven memory:

```bash
export MAVEN_OPTS="-Xmx2g -Xms512m"
mvn spring-boot:run
```

### Azure OpenAI Connection Failed

1. Verify API key is correct
2. Check endpoint URL format: `https://YOUR-RESOURCE.openai.azure.com/`
3. Ensure deployment name matches your Azure configuration

## Stopping Services

To stop all locally running services:

1. Press `Ctrl+C` in each terminal window
2. Stop Milvus: `docker-compose down`

Or create a `cleanup.sh` script:
```bash
#!/bin/bash
# Kill all Java processes running the services
pkill -f "github-service"
pkill -f "document-processor-service"
pkill -f "embedding-service"
pkill -f "milvus-service"
pkill -f "orchestrator-service"

# Stop Milvus
docker-compose down
```

## Tips for Development

1. **Hot Reload**: Use Spring DevTools for automatic restarts
   - Already included in dependencies
   - Changes are picked up automatically

2. **Debug Mode**: Run with debug enabled
   ```bash
   mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
   ```

3. **Profile-Specific Configuration**: Use different profiles
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

4. **View Logs**: All logs go to console by default
   - Adjust log level in `application.yml`

5. **Test Individual Endpoints**:
   ```bash
   # Get repositories
   curl "http://localhost:8081/api/github/repositories?organization=your-org&filterKeyword=java"
   
   # Process document
   curl -X POST http://localhost:8082/api/processor/chunk \
     -H "Content-Type: application/json" \
     -d '{"content":"Sample document","maxChunkSize":1000}'
   ```

## Next Steps

Once you have the services running successfully locally:

1. ✅ Test the full sync workflow
2. ✅ Verify data in Milvus collection
3. ✅ Set up scheduled sync (automatic at 8:00 AM)
4. ✅ Deploy to Kubernetes cluster
5. ✅ Configure GitHub Actions CI/CD pipeline

## Summary

You've successfully:
- ✅ Built the project with Java 21
- ✅ Fixed all compilation errors
- ✅ Configured environment variables
- ✅ Started Milvus vector database
- ✅ Run all microservices locally

The application is now ready for local development and testing!

