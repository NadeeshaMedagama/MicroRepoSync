# 🚀 Local Development Setup Guide

This guide explains how to run the RepoSync application locally and manually update the Milvus collection.

## 📋 Prerequisites

- **Java 21** (OpenJDK or Oracle)
- **Maven 3.8+**
- **Docker & Docker Compose**
- **Git**
- **curl** (for testing endpoints)

Verify prerequisites:
```bash
java -version   # Should show Java 21
mvn -version    # Should show Maven 3.8+
docker --version
docker compose version
```

---

## 🔧 Step 1: Configure Environment Variables

### Option A: Using Docker Compose (Recommended)

1. **Copy the example environment file:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` with your actual credentials:**
   ```bash
   nano .env   # or use your preferred editor
   ```

3. **Fill in the required values:**
   ```dotenv
   # GitHub - Get token from: https://github.com/settings/tokens
   REPOSYNC_GITHUB_TOKEN=ghp_your_actual_token_here
   REPOSYNC_ORGANIZATION=your-org-name
   REPOSYNC_FILTER_KEYWORD=          # Optional: filter repos by keyword

   # Azure OpenAI - Get from Azure Portal
   AZURE_OPENAI_API_KEY=your-azure-key
   AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
   AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT=text-embedding-ada-002

   # Milvus - Use internal Docker network (already configured)
   MILVUS_URI=http://milvus-standalone:19530
   MILVUS_TOKEN=root:Milvus
   MILVUS_COLLECTION_NAME=reposync_collection
   ```

4. **Important:** Never commit `.env` to version control!
   ```bash
   # Already in .gitignore, but verify:
   grep ".env" .gitignore
   ```

---

## 🏗️ Step 2: Build the Application

```bash
# Build all microservices (skip tests for faster build)
mvn clean package -DskipTests

# Or build with checkstyle disabled:
mvn clean package -DskipTests -Dcheckstyle.skip=true
```

This creates JAR files in each service's `target/` directory.

---

## 🐳 Step 3: Start All Services with Docker Compose

### Start All Services:
```bash
docker compose up -d
```

This starts:
- ✅ Milvus infrastructure (etcd, minio, milvus-standalone)
- ✅ All 5 microservices (github, processor, embedding, milvus, orchestrator)
- ✅ Monitoring stack (prometheus, grafana, monitoring-service)

### View Service Status:
```bash
docker compose ps
```

### View Logs:
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f orchestrator-service
docker compose logs -f milvus-service
```

### Wait for Health Checks:
```bash
# Check orchestrator (waits for all dependencies)
curl http://localhost:8086/actuator/health

# Check individual services
curl http://localhost:8081/actuator/health  # GitHub Service
curl http://localhost:8082/actuator/health  # Document Processor
curl http://localhost:8083/actuator/health  # Embedding Service
curl http://localhost:8084/actuator/health  # Milvus Service
```

---

## 📊 Step 4: Manually Trigger a Sync (Update Milvus Collection)

### Method 1: Using curl (Command Line)

```bash
# Trigger the sync
curl -X POST http://localhost:8086/api/orchestrator/sync \
  -H "Content-Type: application/json" \
  | jq '.'

# Expected successful response:
# {
#   "status": "SUCCESS",
#   "repositoriesProcessed": 5,
#   "documentsProcessed": 150,
#   "chunksCreated": 450,
#   "vectorsStored": 450,
#   "startTime": "2026-01-15T10:30:00Z",
#   "endTime": "2026-01-15T10:35:00Z"
# }
```

### Method 2: Using Postman or Insomnia

- **Method:** POST
- **URL:** `http://localhost:8086/api/orchestrator/sync`
- **Headers:** `Content-Type: application/json`
- **Body:** (empty or `{}`)

### Method 3: Using the Monitoring Dashboard

1. Open Grafana: http://localhost:3030
2. Default credentials: `admin` / `admin`
3. View sync metrics in real-time

---

## 🔍 Step 5: Verify Data in Milvus

### Option A: Using the Milvus Service API

```bash
# Get collection stats
curl http://localhost:8084/api/milvus/stats

# Search for documents (example)
curl -X POST http://localhost:8084/api/milvus/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "authentication implementation",
    "topK": 5
  }' | jq '.'
```

### Option B: Using Attu (Milvus Web UI)

1. **Install Attu:**
   ```bash
   docker run -d --name attu \
     --network microservices_with_reposync_reposync-network \
     -p 8000:3000 \
     -e MILVUS_URL=milvus-standalone:19530 \
     zilliz/attu:latest
   ```

2. **Access:** http://localhost:8000
3. **Connect:** 
   - Host: `milvus-standalone`
   - Port: `19530`
   - Username: `root`
   - Password: `Milvus`

### Option C: Using Python Milvus Client

```python
from pymilvus import connections, Collection

# Connect
connections.connect(
    alias="default",
    host='localhost',
    port='19530',
    user='root',
    password='Milvus'
)

# Get collection
collection = Collection("reposync_collection")
collection.load()

# Get stats
print(f"Number of entities: {collection.num_entities}")

# Search example
results = collection.search(
    data=[[0.1, 0.2, ...]],  # Your embedding vector
    anns_field="embedding",
    param={"metric_type": "L2", "params": {"nprobe": 10}},
    limit=10
)
```

---

## 🎯 Common Use Cases

### 1. Sync Specific Organization
Edit `.env`:
```dotenv
REPOSYNC_ORGANIZATION=microsoft
REPOSYNC_FILTER_KEYWORD=typescript
```
Restart: `docker compose restart orchestrator-service`

### 2. Clear and Re-sync Collection
```bash
# Stop services
docker compose down

# Remove Milvus data volumes
docker volume rm microservices_with_reposync_milvus-data

# Restart everything
docker compose up -d

# Wait for health, then trigger sync
curl -X POST http://localhost:8086/api/orchestrator/sync
```

### 3. Debug a Failed Sync
```bash
# Check orchestrator logs
docker compose logs --tail=200 orchestrator-service

# Check which service failed
docker compose ps

# Check specific service logs
docker compose logs milvus-service
docker compose logs embedding-service
```

### 4. Test Individual Services
```bash
# Test GitHub service
curl http://localhost:8081/api/github/repos?organization=microsoft

# Test Document Processor
curl -X POST http://localhost:8082/api/processor/process \
  -H "Content-Type: application/json" \
  -d '{"content": "# Test\nSample code", "path": "test.md"}'

# Test Embedding Service
curl -X POST http://localhost:8083/api/embedding/generate \
  -H "Content-Type: application/json" \
  -d '{"texts": ["hello world"]}'
```

---

## 🛑 Step 6: Stop and Clean Up

### Stop Services (Keep Data):
```bash
docker compose down
```

### Stop and Remove All Data:
```bash
docker compose down -v
```

### Remove Built Images:
```bash
docker compose down --rmi all -v
```

---

## 🔧 Troubleshooting

### Issue: Services won't start
```bash
# Check logs
docker compose logs

# Check if ports are already in use
lsof -i :8080  # Orchestrator
lsof -i :19530 # Milvus

# Rebuild images
docker compose build --no-cache
docker compose up -d
```

### Issue: Milvus connection fails
```bash
# Check Milvus is running
docker compose ps milvus-standalone

# Check Milvus logs
docker compose logs milvus-standalone

# Restart Milvus stack
docker compose restart milvus-etcd milvus-minio milvus-standalone milvus-service
```

### Issue: Out of Memory
```bash
# Check Docker resources
docker stats

# Increase Docker memory limit in Docker Desktop settings
# Recommended: At least 8GB RAM for all services
```

### Issue: "Cannot connect to Docker daemon"
```bash
# Start Docker Desktop (on Mac/Windows)
# Or start Docker service (on Linux)
sudo systemctl start docker
```

### Issue: Log4j2 Warning During Build
If you see this warning during Maven build:
```
ERROR StatusLogger Log4j2 could not find a logging implementation...
```

**This is harmless and won't affect functionality.** To suppress it:
```bash
mvn clean package -DskipTests -Dorg.slf4j.simpleLogger.defaultLogLevel=WARN
```

Or ignore it - the build will still succeed.

---

## 📚 Additional Resources

### Service Ports
- **8080** - Orchestrator Service (Main API)
- **8081** - GitHub Service
- **8082** - Document Processor Service
- **8083** - Embedding Service
- **8084** - Milvus Service
- **8085** - Monitoring Service
- **3000** - Grafana Dashboard
- **9090** - Prometheus
- **19530** - Milvus gRPC
- **9091** - Milvus Metrics

### API Documentation
Access Swagger UI (if enabled):
- http://localhost:8086/swagger-ui.html

### Monitoring
- **Grafana:** http://localhost:3030 (admin/admin)
- **Prometheus:** http://localhost:9090

### Useful Commands
```bash
# Rebuild single service
docker compose build orchestrator-service
docker compose up -d orchestrator-service

# View resource usage
docker stats

# Clean up unused Docker resources
docker system prune -a

# Export logs
docker compose logs > all-services.log
```

---

## 🎓 Development Workflow

1. **Make code changes** in your IDE
2. **Rebuild affected service:**
   ```bash
   mvn clean package -pl orchestrator-service -am -DskipTests
   ```
3. **Restart the service:**
   ```bash
   docker compose up -d --build orchestrator-service
   ```
4. **Test changes:**
   ```bash
   curl http://localhost:8086/actuator/health
   ```
5. **View logs:**
   ```bash
   docker compose logs -f orchestrator-service
   ```

---

## ✅ Quick Start (TL;DR)

```bash
# 1. Setup
cp .env.example .env
nano .env  # Fill in your credentials

# 2. Build
mvn clean package -DskipTests

# 3. Start
docker compose up -d

# 4. Wait for health
sleep 60
curl http://localhost:8086/actuator/health

# 5. Trigger sync
curl -X POST http://localhost:8086/api/orchestrator/sync | jq '.'

# 6. Stop
docker compose down
```

---

**Need help?** Check the logs: `docker compose logs -f`

**Having issues?** See the troubleshooting section above.

