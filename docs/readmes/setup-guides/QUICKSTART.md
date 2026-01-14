# 🎯 QUICKSTART - Run RepoSync Locally

The fastest way to run RepoSync microservices on your local machine.

## ⚡ 3 Steps to Run

### Step 1: Configure Environment (2 min)

```bash
# Navigate to project directory
cd /home/nadeeshame/IdeaProjects/Microservices_with_RepoSync

# Copy environment template
cp .env.example .env

# Edit with your actual credentials
nano .env  # or use any text editor
```

**Required values to fill in .env:**
- `REPOSYNC_GITHUB_TOKEN` - Get from GitHub → Settings → Developer settings → Personal access tokens
- `REPOSYNC_ORGANIZATION` - Your GitHub organization name
- `AZURE_OPENAI_API_KEY` - From Azure Portal → Your OpenAI resource → Keys
- `AZURE_OPENAI_ENDPOINT` - From Azure Portal (e.g., https://your-resource.openai.azure.com/)
- `AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT` - Your deployment name (e.g., text-embedding-ada-002)

### Step 2: Run Quick Start Script (3-5 min)

```bash
./quick-start.sh
```

The script will automatically:
1. Check prerequisites (Java 21, Maven, Docker)
2. Verify .env configuration
3. Build the project
4. Give you options to run services

**Choose Option 1** (Docker Compose) when prompted.

### Step 3: Verify Everything Works (1 min)

```bash
# Check all services are up
docker-compose ps

# Test the orchestrator
curl -X POST http://localhost:8080/api/orchestrator/sync

# Watch logs
docker-compose logs -f
```

**Success!** Your microservices are now running! 🎉

---

## 🚀 Alternative: Manual Start

If you prefer to run commands manually:

```bash
# 1. Build project
mvn clean install

# 2. Load environment variables
export $(cat .env | grep -v '^#' | xargs)

# 3. Start all services with Docker Compose
docker-compose up -d

# 4. Check status
docker-compose ps

# 5. Trigger sync
curl -X POST http://localhost:8080/api/orchestrator/sync
```

---

## 🌐 Access Points

| Service | URL |
|---------|-----|
| Orchestrator | http://localhost:8080 |
| GitHub Service | http://localhost:8081 |
| Document Processor | http://localhost:8082 |
| Embedding Service | http://localhost:8083 |
| Milvus Service | http://localhost:8084 |

**Health Checks:**
```bash
curl http://localhost:8080/actuator/health
```

---

## 🛑 Stopping Services

```bash
docker-compose down
```

---

## 📚 More Information

- **Detailed Setup Guide**: [LOCAL_SETUP_GUIDE.md](LOCAL_SETUP_GUIDE.md)
- **Setup Checklist**: [SETUP_CHECKLIST.md](SETUP_CHECKLIST.md)
- **Architecture Details**: [README.md](../../README.md)

---

## ❓ Common Issues

**Build fails:**
```bash
java --version  # Ensure it shows 21.x.x
mvn clean install -U
```

**Port in use:**
```bash
lsof -i :8080
kill -9 <PID>
```

**Services won't start:**
```bash
docker-compose down
docker-compose up -d
```

---

**Need help?** Check the detailed guides above or review the logs with `docker-compose logs -f`

