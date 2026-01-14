# 🚀 Local Setup and Run Guide - RepoSync Microservices

This guide will help you run the RepoSync Microservices project on your local machine.

## 📋 Prerequisites Checklist

Before starting, ensure you have:

- ✅ **Java 21** (OpenJDK 21 or higher) - Already installed ✓
- ✅ **Maven 3.6+** 
- ✅ **Docker & Docker Compose** (for running Milvus and containerized services)
- ✅ **GitHub Personal Access Token** with repo access
- ✅ **Azure OpenAI API Key** and endpoint
- ✅ **Milvus instance** (can run locally via Docker)

## 🔧 Step 1: Verify Your Environment

```bash
# Check Java version (should be 21)
java --version

# Check Maven
mvn --version

# Check Docker
docker --version
docker-compose --version
```

## 📝 Step 2: Configure Environment Variables

1. **Copy the example environment file:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` file with your credentials:**
   ```bash
   nano .env  # or use your preferred editor
   ```

3. **Fill in the following values:**

   ```env
   # GitHub Configuration
   REPOSYNC_GITHUB_TOKEN=ghp_YOUR_ACTUAL_TOKEN_HERE
   REPOSYNC_ORGANIZATION=your-github-org-name
   REPOSYNC_FILTER_KEYWORD=microservices  # or any keyword to filter repos
   
   # Azure OpenAI Configuration
   AZURE_OPENAI_API_KEY=your-actual-azure-openai-key
   AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
   AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT=text-embedding-ada-002
   
   # Milvus Configuration
   MILVUS_URI=localhost:19530
   MILVUS_TOKEN=  # Leave empty for local Milvus
   MILVUS_COLLECTION_NAME=reposync_collection
   
   # Service URLs (for local development)
   GITHUB_SERVICE_URL=http://localhost:8081
   PROCESSOR_SERVICE_URL=http://localhost:8082
   EMBEDDING_SERVICE_URL=http://localhost:8083
   MILVUS_SERVICE_URL=http://localhost:8084
   ```

**How to get credentials:**
- **GitHub Token**: Go to GitHub → Settings → Developer settings → Personal access tokens → Generate new token (classic) → Select "repo" scope
- **Azure OpenAI**: Get from Azure Portal → Your OpenAI resource → Keys and Endpoint

## 🏗️ Step 3: Build the Project

```bash
# Navigate to project root
cd /home/nadeeshame/IdeaProjects/Microservices_with_RepoSync

# Clean and build all services
mvn clean install

# This will:
# - Compile all Java code
# - Run tests
# - Package JAR files for each service
# - Takes about 2-5 minutes on first run
```

If build fails, check:
- Java version is 21
- All dependencies can be downloaded
- No compilation errors in the code

## 🐳 Step 4: Choose Your Running Method

You have **3 options** to run the project locally:

---

### **Option A: Docker Compose (Easiest - Recommended)**

This runs everything in containers including Milvus.

```bash
# Start all services
docker-compose up -d

# Check if all containers are running
docker-compose ps

# View logs of all services
docker-compose logs -f

# View logs of specific service
docker-compose logs -f orchestrator-service

# Stop all services
docker-compose down
```

**Services will be available at:**
- Orchestrator: http://localhost:8080
- GitHub Service: http://localhost:8081
- Document Processor: http://localhost:8082
- Embedding Service: http://localhost:8083
- Milvus Service: http://localhost:8084

---

### **Option B: Run Services Individually (For Development)**

This gives you more control and is better for debugging.

#### 4.1: Start Milvus Database First

```bash
# Start Milvus standalone
docker run -d \
  --name milvus-standalone \
  -p 19530:19530 \
  -p 9091:9091 \
  -v milvus-data:/var/lib/milvus \
  milvusdb/milvus:latest \
  milvus run standalone
```

#### 4.2: Start Each Microservice

Open **5 separate terminal windows** and run each service:

**Terminal 1 - GitHub Service:**
```bash
cd /home/nadeeshame/IdeaProjects/Microservices_with_RepoSync/github-service
export REPOSYNC_GITHUB_TOKEN=your_token_here
mvn spring-boot:run
```

**Terminal 2 - Document Processor Service:**
```bash
cd /home/nadeeshame/IdeaProjects/Microservices_with_RepoSync/document-processor-service
mvn spring-boot:run
```

**Terminal 3 - Embedding Service:**
```bash
cd /home/nadeeshame/IdeaProjects/Microservices_with_RepoSync/embedding-service
export AZURE_OPENAI_API_KEY=your_key_here
export AZURE_OPENAI_ENDPOINT=your_endpoint_here
export AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT=text-embedding-ada-002
mvn spring-boot:run
```

**Terminal 4 - Milvus Service:**
```bash
cd /home/nadeeshame/IdeaProjects/Microservices_with_RepoSync/milvus-service
export MILVUS_URI=localhost:19530
export MILVUS_COLLECTION_NAME=reposync_collection
mvn spring-boot:run
```

**Terminal 5 - Orchestrator Service:**
```bash
cd /home/nadeeshame/IdeaProjects/Microservices_with_RepoSync/orchestrator-service
export REPOSYNC_ORGANIZATION=your_org_here
export REPOSYNC_FILTER_KEYWORD=your_keyword_here
export MILVUS_COLLECTION_NAME=reposync_collection
export GITHUB_SERVICE_URL=http://localhost:8081
export PROCESSOR_SERVICE_URL=http://localhost:8082
export EMBEDDING_SERVICE_URL=http://localhost:8083
export MILVUS_SERVICE_URL=http://localhost:8084
mvn spring-boot:run
```

**Note:** Easier way is to set all variables in your shell's RC file (~/.bashrc or ~/.zshrc) or use the .env file with `export $(cat .env | xargs)` before running.

---

### **Option C: Using IntelliJ IDEA (Best for Development)**

1. **Open the project in IntelliJ IDEA**
   - File → Open → Select the project root directory

2. **Configure Java 21**
   - File → Project Structure → Project → SDK → Select Java 21
   - File → Project Structure → Project → Language Level → 21

3. **Import Maven Projects**
   - IntelliJ should auto-detect and import all Maven modules
   - Wait for dependencies to download

4. **Configure Environment Variables**
   - Run → Edit Configurations → Add New Configuration → Spring Boot
   - For each service (github-service, document-processor-service, etc.):
     - Main class: `com.reposync.xxx.XxxApplication`
     - Environment variables: Copy from .env file
     - Working directory: Service directory

5. **Start Milvus in Docker**
   ```bash
   docker run -d --name milvus-standalone -p 19530:19530 milvusdb/milvus:latest milvus run standalone
   ```

6. **Run Services in Order**
   - Start github-service
   - Start document-processor-service
   - Start embedding-service
   - Start milvus-service
   - Start orchestrator-service (last)

---

## ✅ Step 5: Verify Services are Running

Check health endpoints:

```bash
# Check all services
curl http://localhost:8080/actuator/health  # Orchestrator
curl http://localhost:8081/actuator/health  # GitHub
curl http://localhost:8082/actuator/health  # Document Processor
curl http://localhost:8083/actuator/health  # Embedding
curl http://localhost:8084/actuator/health  # Milvus
```

All should return: `{"status":"UP"}`

## 🎯 Step 6: Trigger Manual Sync

Once all services are running, trigger a sync:

```bash
# Trigger the sync process
curl -X POST http://localhost:8080/api/orchestrator/sync

# You should see a response like:
# {"status":"success","message":"Sync completed successfully"}
```

Watch the logs to see:
1. Repositories being fetched from GitHub
2. Documents being chunked
3. Embeddings being generated
4. Vectors being stored in Milvus

## 📊 Step 7: Monitor the Process

### View Logs

**Docker Compose:**
```bash
docker-compose logs -f orchestrator-service
```

**Individual Services:**
Check the terminal window where each service is running.

**IntelliJ:**
View logs in the Run console at the bottom.

### Check Service Status

```bash
# See all running containers
docker ps

# Check specific service logs
docker logs orchestrator-service -f
```

## 🐛 Troubleshooting

### Issue: Build fails with "release version not supported"

**Solution:**
```bash
# Verify Java 21 is active
java --version

# Should show: openjdk 21.0.9

# Clean and rebuild
mvn clean install -U
```

### Issue: Port already in use

**Solution:**
```bash
# Find what's using the port
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or use different ports in application.yml
```

### Issue: Cannot connect to Milvus

**Solution:**
```bash
# Check if Milvus is running
docker ps | grep milvus

# Restart Milvus
docker stop milvus-standalone
docker rm milvus-standalone
docker run -d --name milvus-standalone -p 19530:19530 milvusdb/milvus:latest milvus run standalone

# Wait 30 seconds for Milvus to start, then try again
```

### Issue: Service won't start - missing environment variables

**Solution:**
```bash
# Load .env file
export $(cat .env | xargs)

# Or set them manually
export REPOSYNC_GITHUB_TOKEN=your_token
export AZURE_OPENAI_API_KEY=your_key
# ... etc
```

### Issue: GitHub API rate limit exceeded

**Solution:**
- Use a GitHub Personal Access Token (not anonymous)
- Wait for the rate limit to reset (1 hour)
- Reduce the number of repositories by using a more specific filter keyword

### Issue: Azure OpenAI connection errors

**Solution:**
- Verify your API key is correct
- Check endpoint URL format: `https://your-resource.openai.azure.com/`
- Ensure your deployment name matches: `text-embedding-ada-002`
- Check Azure OpenAI quota and rate limits

## 🧪 Testing Individual Services

### Test GitHub Service
```bash
curl "http://localhost:8081/api/github/repositories?organization=YOUR_ORG&filterKeyword=YOUR_KEYWORD"
```

### Test Document Processor Service
```bash
curl -X POST http://localhost:8082/api/processor/chunk \
  -H "Content-Type: application/json" \
  -d '{
    "content": "This is a test document that will be chunked into smaller pieces.",
    "source": "test.md",
    "metadata": {"type": "README"}
  }'
```

### Test Embedding Service
```bash
curl -X POST http://localhost:8083/api/embedding/generate \
  -H "Content-Type: application/json" \
  -d '{
    "text": "This is a test chunk for embedding generation."
  }'
```

### Test Milvus Service
```bash
# Check if collection exists
curl "http://localhost:8084/api/milvus/collection/reposync_collection/exists"
```

## 📚 Next Steps

1. **Customize Configuration:**
   - Edit `application.yml` in each service for chunking size, batch sizes, etc.
   - Modify schedule in orchestrator-service for different sync times

2. **Add More Features:**
   - Implement search API to query the vector database
   - Add monitoring and alerting
   - Create a web UI for visualization

3. **Deploy to Production:**
   - Follow Kubernetes deployment guide in README.md
   - Set up CI/CD pipeline with GitHub Actions

## 🎉 Success!

Your RepoSync Microservices should now be running locally. The system will:
- Fetch repositories from your GitHub organization
- Extract README and API definition files
- Chunk documents intelligently
- Generate embeddings using Azure OpenAI
- Store vectors in Milvus for similarity search

Enjoy your local vector database of GitHub repositories! 🚀

---

**Need Help?**
- Check the main [README.md](../../README.md) for architecture details
- Review logs for specific error messages
- Open an issue on GitHub for bugs or questions

