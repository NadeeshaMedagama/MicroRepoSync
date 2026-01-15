# 🛠️ Development Scripts

This directory contains helpful scripts for local development and manual operations.

## 📋 Available Scripts

### 🚀 `start-local.sh`
**Purpose:** One-command setup to build and start all services locally

**Usage:**
```bash
./scripts/start-local.sh
```

**What it does:**
1. ✅ Checks prerequisites (Java 21, Maven, Docker)
2. ✅ Validates `.env` configuration
3. ✅ Builds all microservices with Maven
4. ✅ Starts Docker Compose with all services
5. ✅ Waits for services to be healthy
6. ✅ Displays access points and next steps

**When to use:**
- First time setup
- After pulling new code changes
- When starting a new development session

---

### 🔄 `trigger-sync.sh`
**Purpose:** Manually trigger a repository sync to update the Milvus collection

**Usage:**
```bash
./scripts/trigger-sync.sh
```

**What it does:**
1. ✅ Checks if Orchestrator Service is running
2. ✅ Shows current configuration (org, filters, collection)
3. ✅ Asks for confirmation
4. ✅ Triggers the sync via REST API
5. ✅ Displays real-time results and statistics

**When to use:**
- After configuring a new organization in `.env`
- To refresh repository data
- To test the end-to-end pipeline
- After code changes to sync logic

**Example Output:**
```
✅ Sync completed successfully!

Statistics:
  Repositories: 5
  Documents:    150
  Chunks:       450
  Vectors:      450
```

---

### 📊 `check-milvus.sh`
**Purpose:** Check the current status and statistics of your Milvus collection

**Usage:**
```bash
./scripts/check-milvus.sh
```

**What it does:**
1. ✅ Verifies Milvus Service is running
2. ✅ Displays collection statistics
3. ✅ Shows service health details
4. ✅ Lists Milvus container status

**When to use:**
- Verify data was successfully stored
- Check collection size
- Debug Milvus connectivity issues
- Before/after running a sync

---

### 🧪 `test-integration.sh`
**Purpose:** Run integration tests across all services

**Usage:**
```bash
./scripts/test-integration.sh
```

**When to use:**
- Before committing code changes
- To verify end-to-end functionality
- After infrastructure changes

---

### 🔧 `diagnose-sync-failure.sh`
**Purpose:** Debug failed sync operations

**Usage:**
```bash
./scripts/diagnose-sync-failure.sh
```

**What it does:**
1. Checks all service health endpoints
2. Displays recent logs from all services
3. Validates environment configuration
4. Suggests fixes based on common issues

**When to use:**
- Sync job failed
- Services won't start
- Connection errors between services

---

## 🎯 Common Workflows

### First-Time Setup
```bash
# 1. Copy and configure environment
cp .env.example .env
nano .env  # Add your credentials

# 2. Start everything
./scripts/start-local.sh

# 3. Trigger initial sync
./scripts/trigger-sync.sh

# 4. Verify data
./scripts/check-milvus.sh
```

### Daily Development
```bash
# Start services
./scripts/start-local.sh

# Make code changes...

# Rebuild and test
mvn clean package -pl orchestrator-service -am -DskipTests
docker compose up -d --build orchestrator-service

# Test manually
./scripts/trigger-sync.sh
```

### Debugging a Failed Sync
```bash
# 1. Check what went wrong
./scripts/diagnose-sync-failure.sh

# 2. View detailed logs
docker compose logs -f orchestrator-service

# 3. Check individual services
docker compose logs milvus-service
docker compose logs embedding-service

# 4. Restart problematic service
docker compose restart milvus-service
```

### Clean Reset
```bash
# Stop everything and delete data
docker compose down -v

# Start fresh
./scripts/start-local.sh

# Re-sync
./scripts/trigger-sync.sh
```

---

## 🔍 Script Locations

All scripts are in the `/scripts` directory:
```
scripts/
├── start-local.sh          # Main startup script
├── trigger-sync.sh         # Manual sync trigger
├── check-milvus.sh         # Collection status
├── test-integration.sh     # Integration tests
├── diagnose-sync-failure.sh # Debugging tool
├── deploy-k8s.sh          # Kubernetes deployment
├── start-monitoring.sh    # Monitoring stack
└── README.md              # This file
```

---

## 💡 Tips

### Make Scripts Globally Accessible
Add to your `.bashrc` or `.zshrc`:
```bash
export PATH="$PATH:/home/nadeeshame/IdeaProjects/Microservices_with_RepoSync/scripts"
```

Then you can run from anywhere:
```bash
start-local.sh
trigger-sync.sh
```

### Create Aliases
```bash
alias rs-start='./scripts/start-local.sh'
alias rs-sync='./scripts/trigger-sync.sh'
alias rs-status='./scripts/check-milvus.sh'
alias rs-logs='docker compose logs -f orchestrator-service'
```

### Quick Health Check
```bash
# Check if everything is running
docker compose ps

# Quick health check
for port in 8080 8081 8082 8083 8084; do
  echo -n "Port $port: "
  curl -sf http://localhost:$port/actuator/health && echo "✅" || echo "❌"
done
```

---

## 🆘 Troubleshooting

### "Permission denied" error
```bash
chmod +x scripts/*.sh
```

### "Command not found: jq"
```bash
# Ubuntu/Debian
sudo apt-get install jq

# macOS
brew install jq
```

### "Cannot connect to Docker daemon"
```bash
# Start Docker Desktop (macOS/Windows)
# Or start Docker service (Linux)
sudo systemctl start docker
```

### Scripts hang during health check
```bash
# Check if ports are blocked
lsof -i :8080

# Check Docker resources
docker stats

# View what's happening
docker compose logs
```

---

## 📚 Additional Documentation

- **Full Setup Guide:** `../LOCAL_SETUP_GUIDE.md`
- **API Documentation:** Check Swagger UI at http://localhost:8080/swagger-ui.html
- **Monitoring Guide:** `../docs/readmes/monitoring/`
- **Deployment Guide:** `../docs/readmes/build-deployment/`

---

**Need help?** Check the main documentation or run with `-h` flag (if implemented).

