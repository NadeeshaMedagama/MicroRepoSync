# ✅ First-Time Setup Checklist

Use this checklist to ensure you have everything needed to run RepoSync locally.

## Prerequisites

- [ ] **Java 21 installed**
  ```bash
  java --version  # Should show 21.x.x
  ```

- [ ] **Maven 3.6+ installed**
  ```bash
  mvn --version
  ```

- [ ] **Docker installed** (for Milvus and Docker Compose option)
  ```bash
  docker --version
  docker-compose --version
  ```

## Credentials Setup

### GitHub Token
- [ ] Go to GitHub → Settings → Developer settings → Personal access tokens
- [ ] Click "Generate new token (classic)"
- [ ] Select scopes:
  - [x] `repo` - Full control of private repositories
  - [x] `read:org` - Read org and team membership
- [ ] Copy the token (starts with `ghp_`)

### Azure OpenAI
- [ ] Access Azure Portal
- [ ] Navigate to your OpenAI resource
- [ ] Get from "Keys and Endpoint" section:
  - [ ] API Key
  - [ ] Endpoint URL (e.g., `https://your-resource.openai.azure.com/`)
  - [ ] Deployment name (e.g., `text-embedding-ada-002`)

### Milvus
For local development:
- [ ] Use `localhost:19530` as URI
- [ ] Leave token empty
- [ ] Choose a collection name (e.g., `reposync_collection`)

## Configuration

- [ ] Copy `.env.example` to `.env`
  ```bash
  cp .env.example .env
  ```

- [ ] Fill in `.env` with your credentials:
  ```env
  REPOSYNC_GITHUB_TOKEN=ghp_your_actual_token_here
  REPOSYNC_ORGANIZATION=your-github-org-name
  REPOSYNC_FILTER_KEYWORD=microservices
  
  AZURE_OPENAI_API_KEY=your_actual_api_key
  AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
  AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT=text-embedding-ada-002
  
  MILVUS_URI=localhost:19530
  MILVUS_TOKEN=
  MILVUS_COLLECTION_NAME=reposync_collection
  ```

- [ ] Verify no example values remain:
  ```bash
  grep -E "your_|example" .env
  # Should return nothing
  ```

## Build Project

- [ ] Clean build all services:
  ```bash
  mvn clean install
  ```

- [ ] Verify build success:
  ```
  [INFO] BUILD SUCCESS
  [INFO] Total time: ...
  ```

- [ ] Check JAR files created:
  ```bash
  ls -lh */target/*.jar
  ```

## Choose Run Method

Pick ONE of the following:

### Option A: Docker Compose
- [ ] Start all services:
  ```bash
  docker-compose up -d
  ```
- [ ] Verify all containers running:
  ```bash
  docker-compose ps
  ```

### Option B: Individual Services
- [ ] Start Milvus:
  ```bash
  docker run -d --name milvus-standalone -p 19530:19530 milvusdb/milvus:latest milvus run standalone
  ```
- [ ] Load environment:
  ```bash
  export $(cat .env | grep -v '^#' | xargs)
  ```
- [ ] Start each service in separate terminal (see guide)

### Option C: IntelliJ IDEA
- [ ] Open project in IntelliJ
- [ ] Configure Java 21 SDK
- [ ] Import Maven projects
- [ ] Create run configurations for each service
- [ ] Start Milvus in Docker
- [ ] Run all services

## Verification

- [ ] All services health checks pass:
  ```bash
  curl http://localhost:8086/actuator/health  # Orchestrator
  curl http://localhost:8081/actuator/health  # GitHub
  curl http://localhost:8082/actuator/health  # Document Processor
  curl http://localhost:8083/actuator/health  # Embedding
  curl http://localhost:8084/actuator/health  # Milvus
  ```

- [ ] All return `{"status":"UP"}`

- [ ] Services are listening on correct ports:
  ```bash
  netstat -tuln | grep -E "8080|8081|8082|8083|8084|19530"
  ```

## Test Run

- [ ] Trigger manual sync:
  ```bash
  curl -X POST http://localhost:8086/api/orchestrator/sync
  ```

- [ ] Check response is successful

- [ ] Monitor logs for:
  - [ ] Repositories fetched
  - [ ] Documents extracted
  - [ ] Chunks created
  - [ ] Embeddings generated
  - [ ] Vectors stored in Milvus

- [ ] No errors in logs

## Optional: Test Individual Services

- [ ] Test GitHub Service:
  ```bash
  curl "http://localhost:8081/api/github/repositories?organization=YOUR_ORG&filterKeyword=YOUR_KEYWORD"
  ```

- [ ] Test Document Processor:
  ```bash
  curl -X POST http://localhost:8082/api/processor/chunk \
    -H "Content-Type: application/json" \
    -d '{"content":"Test document","source":"test.md","metadata":{}}'
  ```

- [ ] Test Embedding Service:
  ```bash
  curl -X POST http://localhost:8083/api/embedding/generate \
    -H "Content-Type: application/json" \
    -d '{"text":"Test text"}'
  ```

- [ ] Test Milvus Service:
  ```bash
  curl "http://localhost:8084/api/milvus/collection/reposync_collection/exists"
  ```

## Troubleshooting

If anything doesn't work:

- [ ] Check Java version is 21
- [ ] Verify all environment variables are set
- [ ] Ensure no ports are already in use
- [ ] Check Milvus is running
- [ ] Review service logs for errors
- [ ] Verify credentials are correct
- [ ] Check network connectivity

## Common Issues Checklist

- [ ] **Build fails**: Java version mismatch → Use Java 21
- [ ] **Port in use**: Another process → Kill process or change port
- [ ] **Milvus connection**: Not running → Start Milvus container
- [ ] **GitHub API**: Rate limit → Use personal access token
- [ ] **Azure OpenAI**: Invalid credentials → Verify key and endpoint
- [ ] **Environment vars**: Not loaded → Export from .env file

## Success Criteria

You know everything works when:

✅ All 5 microservices start without errors
✅ All health checks return UP status
✅ Manual sync completes successfully
✅ Logs show:
  - Repositories fetched
  - Documents chunked
  - Embeddings generated
  - Vectors stored
✅ No error messages in logs
✅ Milvus collection created and populated

## Next Steps

Once everything works:

- [ ] Read [LOCAL_SETUP_GUIDE.md](LOCAL_SETUP_GUIDE.md) for detailed info
- [ ] Review [README.md](../../README.md) for architecture details
- [ ] Customize configuration in `application.yml` files
- [ ] Set up GitHub Actions for automated daily sync
- [ ] Deploy to Kubernetes cluster (see README)
- [ ] Implement search functionality for querying vectors

---

**Congratulations! Your local setup is complete! 🎉**

If you encounter any issues not covered here, check the logs and the troubleshooting section in LOCAL_SETUP_GUIDE.md.

