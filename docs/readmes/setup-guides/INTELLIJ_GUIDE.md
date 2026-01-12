# ✅ Running the Project Locally with IntelliJ IDEA (Java 21)

## 🎉 Project is Now Configured for Java 21!

All project files have been updated to use Java 21. Here's how to run it in IntelliJ IDEA.

---

## 🚀 Quick Start in IntelliJ IDEA

### Step 1: Configure IntelliJ for Java 21

1. **Set Project SDK**:
   - File → Project Structure → Project
   - SDK: Select "21" (openjdk-21)
   - Language Level: 21 - Record patterns, pattern matching for switch
   - Click Apply

2. **Set Maven JDK**:
   - File → Settings → Build, Execution, Deployment → Build Tools → Maven → Runner
   - JRE: Select "Use Project JDK" or explicitly select Java 21
   - Click Apply

### Step 2: Reload Maven Project

1. Open Maven tool window (View → Tool Windows → Maven)
2. Click the "Reload All Maven Projects" button (circular arrows icon)
3. Wait for dependencies to download (first time may take 2-3 minutes)

### Step 3: Build the Project

**Option A: Using IntelliJ Build**
- Build → Build Project (Ctrl+F9)

**Option B: Using Maven in IntelliJ**
- Maven tool window → microservices-reposync → Lifecycle → clean
- Then: Lifecycle → install
- IntelliJ's Maven will use Java 21 automatically

### Step 4: Configure Environment Variables

Create a `.env` file (or configure in Run Configurations):

```bash
cd /home/nadeeshame/IdeaProjects/Microservices_with_RepoSync
cp .env.example .env
nano .env
```

Add your credentials:
```env
REPOSYNC_GITHUB_TOKEN=ghp_your_github_token
REPOSYNC_ORGANIZATION=your-org-name
REPOSYNC_FILTER_KEYWORD=optional-keyword
AZURE_OPENAI_API_KEY=your-azure-key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT=text-embedding-ada-002
MILVUS_URI=localhost:19530
MILVUS_TOKEN=
MILVUS_COLLECTION_NAME=reposync_collection
```

### Step 5: Run Services

#### Option A: Run All Services Individually (Recommended for Development)

1. **Start Milvus** (using Docker):
   ```bash
   docker-compose up -d milvus-standalone milvus-etcd milvus-minio
   ```

2. **Run GitHub Service**:
   - Navigate to: `github-service/src/main/java/com/reposync/github/GitHubServiceApplication.java`
   - Right-click → Run 'GitHubServiceApplication'
   - Service starts on port 8081

3. **Run Document Processor Service**:
   - Navigate to: `document-processor-service/src/main/java/com/reposync/processor/DocumentProcessorApplication.java`
   - Right-click → Run 'DocumentProcessorApplication'
   - Service starts on port 8082

4. **Run Embedding Service**:
   - Navigate to: `embedding-service/src/main/java/com/reposync/embedding/EmbeddingServiceApplication.java`
   - Right-click → Run 'EmbeddingServiceApplication'
   - Service starts on port 8083

5. **Run Milvus Service**:
   - Navigate to: `milvus-service/src/main/java/com/reposync/milvus/MilvusServiceApplication.java`
   - Right-click → Run 'MilvusServiceApplication'
   - Service starts on port 8084

6. **Run Orchestrator Service**:
   - Navigate to: `orchestrator-service/src/main/java/com/reposync/orchestrator/OrchestratorServiceApplication.java`
   - Right-click → Run 'OrchestratorServiceApplication'
   - Service starts on port 8080

#### Option B: Use Docker Compose (Easiest)

In IntelliJ's terminal or external terminal:
```bash
docker-compose up --build -d
```

This starts everything including Milvus database.

---

## ✅ Verify Services are Running

### In IntelliJ:
- You'll see 5 separate Run tabs at the bottom
- Each showing the service's startup logs
- Look for "Started *Application in X seconds"

### Via Terminal:
```bash
# Health checks
curl http://localhost:8081/actuator/health  # GitHub Service
curl http://localhost:8082/actuator/health  # Document Processor
curl http://localhost:8083/actuator/health  # Embedding Service
curl http://localhost:8084/actuator/health  # Milvus Service
curl http://localhost:8080/actuator/health  # Orchestrator

# Should all return: {"status":"UP"}
```

---

## 🎯 Trigger a Sync Job

Once all services are running:

```bash
curl -X POST http://localhost:8080/api/orchestrator/sync \
  -H "Content-Type: application/json"
```

Watch the logs in IntelliJ to see the sync workflow in action!

---

## 🐛 Troubleshooting

### If Maven Build Fails in IntelliJ

1. **Invalidate Caches**: File → Invalidate Caches → Invalidate and Restart
2. **Reimport Maven**: Maven tool window → Right-click on project → Reimport
3. **Check JDK**: Ensure IntelliJ is using Java 21 for both Project SDK and Maven Runner

### If Services Won't Start

1. **Check Ports**: Ensure ports 8080-8084 are not in use
   ```bash
   netstat -tuln | grep '808[0-4]'
   ```

2. **Check Environment Variables**: 
   - In IntelliJ Run Configuration, add environment variables
   - Or use `.env` file (Spring Boot loads it automatically with proper config)

3. **Check Milvus**: 
   ```bash
   docker ps | grep milvus
   ```
   If not running: `docker-compose up -d milvus-standalone`

### If "Module not found" Errors

1. File → Project Structure → Modules
2. Ensure all 6 modules are listed (parent + 5 services + common-lib)
3. Click Apply

---

## 💡 Development Tips

### Use IntelliJ's Compound Run Configuration

1. Run → Edit Configurations → + → Compound
2. Add all 5 services to the compound
3. Name it "All RepoSync Services"
4. Now you can start all services with one click!

### Enable Live Reload

Add to each service's `pom.xml` (already included):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
```

### Debug Mode

- Set breakpoints in any service
- Right-click → Debug instead of Run
- Step through the workflow!

---

## 📊 What Happens When You Run

1. **GitHub Service** waits for requests to fetch repos
2. **Document Processor** waits for documents to chunk
3. **Embedding Service** waits for chunks to embed
4. **Milvus Service** waits for vectors to store
5. **Orchestrator Service**:
   - Starts HTTP server on 8080
   - Schedules daily sync for 8:00 AM
   - Waits for manual trigger or scheduled time

---

## ✅ Success Checklist

- [ ] IntelliJ configured with Java 21
- [ ] Maven project reloaded successfully
- [ ] Project builds without errors (Ctrl+F9)
- [ ] `.env` file configured with credentials
- [ ] All 5 services running (or Docker Compose up)
- [ ] Health checks return `{"status":"UP"}`
- [ ] Sync job triggered successfully

---

## 🎉 You're Ready!

Your IntelliJ IDEA is now set up to develop and run the RepoSync microservices with Java 21. 

**For local development**: Run services in IntelliJ for easy debugging
**For testing**: Use Docker Compose for full integration testing
**For production**: Deploy to Kubernetes using the provided manifests

Happy coding! 🚀

