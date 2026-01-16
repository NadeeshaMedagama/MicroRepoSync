# 🚀 RepoSync - Quick Start Guide

## One-Line Local Setup

```bash
cp .env.example .env && nano .env && ./scripts/start-local.sh && ./scripts/trigger-sync.sh
```

---

## Essential Commands

### 🎬 Start Everything
```bash
./scripts/start-local.sh
```

### 🔄 Trigger Manual Sync
```bash
./scripts/trigger-sync.sh
```
Or via curl:
```bash
curl -X POST http://localhost:8080/api/orchestrator/sync | jq '.'
```

### 📊 Check Milvus Status
```bash
./scripts/check-milvus.sh
```

### 🛑 Stop Everything
```bash
docker compose down
```

### 🧹 Stop + Delete All Data
```bash
docker compose down -v
```

---

## Service URLs

| Service | URL | Purpose |
|---------|-----|---------|
| Orchestrator | http://localhost:8086 | Main API |
| GitHub | http://localhost:8081 | Fetch repos |
| Processor | http://localhost:8082 | Parse docs |
| Embedding | http://localhost:8083 | Generate vectors |
| Milvus | http://localhost:8084 | Store vectors |
| Grafana | http://localhost:3000 | Monitoring (admin/admin) |
| Prometheus | http://localhost:9090 | Metrics |

---

## Quick Health Check

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

Or all at once:
```bash
for port in 8080 8081 8082 8083 8084; do
  echo -n "Port $port: "
  curl -sf http://localhost:$port/actuator/health && echo "✅" || echo "❌"
done
```

---

## View Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f orchestrator-service
docker compose logs -f milvus-service
docker compose logs -f embedding-service

# Last 100 lines
docker compose logs --tail=100 orchestrator-service
```

---

## Configuration (.env)

```bash
# Edit configuration
nano .env

# Required variables:
REPOSYNC_GITHUB_TOKEN=ghp_your_token_here
AZURE_OPENAI_API_KEY=your_key_here
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/

# Optional:
REPOSYNC_ORGANIZATION=microsoft
REPOSYNC_FILTER_KEYWORD=typescript
```

---

## Rebuild After Code Changes

```bash
# Rebuild all
mvn clean package -DskipTests
docker compose up -d --build

# Rebuild single service
mvn clean package -pl orchestrator-service -am -DskipTests
docker compose up -d --build orchestrator-service
```

---

## Troubleshooting

### Log4j2 Warning During Build
If you see `ERROR StatusLogger Log4j2 could not find a logging implementation...`:
- **This is completely harmless** - it's a Maven build-time warning
- Your services have proper logging configured
- The warning doesn't affect functionality
- Our scripts filter it out automatically

### Services won't start
```bash
docker compose down -v
docker compose build --no-cache
docker compose up -d
```

### Check what's wrong
```bash
docker compose ps
docker compose logs
```

### Milvus issues
```bash
docker compose logs milvus-standalone
docker compose restart milvus-service
```

### Port already in use
```bash
lsof -i :8080
# Kill the process or change port in docker-compose.yml
```

---

## Development Workflow

```bash
# 1. Start
./scripts/start-local.sh

# 2. Make changes in IDE

# 3. Rebuild
mvn package -pl <service-name> -am -DskipTests
docker compose up -d --build <service-name>

# 4. Test
./scripts/trigger-sync.sh

# 5. Check logs
docker compose logs -f <service-name>
```

---

## Full Documentation

📖 **Detailed Guide:** `LOCAL_SETUP_GUIDE.md`
📜 **Scripts Guide:** `scripts/README.md`
🏗️ **Architecture:** `docs/readmes/project-overview/`

---

## Get Help

```bash
# View service status
docker compose ps

# View resource usage
docker stats

# Clean up Docker
docker system prune -a
```

**Need more help?** Check `LOCAL_SETUP_GUIDE.md` for detailed instructions.

