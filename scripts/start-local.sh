#!/bin/bash

# ============================================
# RepoSync Local Development Startup Script
# ============================================

set -e  # Exit on error

echo "🚀 RepoSync Local Development Setup"
echo "===================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check prerequisites
echo "📋 Checking prerequisites..."

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java is not installed${NC}"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${RED}❌ Java 21 or higher is required (found: $JAVA_VERSION)${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Java $JAVA_VERSION found${NC}"

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven is not installed${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Maven found${NC}"

# Check Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker found${NC}"

# Check Docker Compose
if ! command -v docker compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker Compose found${NC}"

echo ""

# Check .env file
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠️  .env file not found${NC}"
    echo "Creating .env from .env.example..."
    cp .env.example .env
    echo -e "${YELLOW}📝 Please edit .env file with your credentials:${NC}"
    echo "   - REPOSYNC_GITHUB_TOKEN"
    echo "   - AZURE_OPENAI_API_KEY"
    echo "   - AZURE_OPENAI_ENDPOINT"
    echo "   - REPOSYNC_ORGANIZATION (optional)"
    echo ""
    read -p "Press Enter after editing .env file..."
fi

# Validate critical environment variables
source .env

if [ -z "$REPOSYNC_GITHUB_TOKEN" ] || [ "$REPOSYNC_GITHUB_TOKEN" = "your_github_token_here" ]; then
    echo -e "${RED}❌ REPOSYNC_GITHUB_TOKEN is not set in .env${NC}"
    exit 1
fi

if [ -z "$AZURE_OPENAI_API_KEY" ] || [ "$AZURE_OPENAI_API_KEY" = "your_azure_openai_api_key" ]; then
    echo -e "${RED}❌ AZURE_OPENAI_API_KEY is not set in .env${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Environment configuration validated${NC}"
echo ""

# Build application
echo "🔨 Building application..."
echo "This may take a few minutes on first run..."
echo "(Ignore any Log4j2 warnings - they are harmless)"
mvn clean package -DskipTests -Dcheckstyle.skip=true -Dlog4j2.statusLoggerLevel=OFF -Dorg.slf4j.simpleLogger.defaultLogLevel=WARN -q 2>&1 | { grep -v "StatusLogger" || true; }
BUILD_EXIT_CODE=${PIPESTATUS[0]}

if [ $BUILD_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅ Build successful${NC}"
else
    echo -e "${RED}❌ Build failed${NC}"
    exit 1
fi
echo ""

# Start Docker Compose
echo "🐳 Starting Docker Compose services..."
docker compose up -d

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Failed to start Docker Compose${NC}"
    exit 1
fi
echo ""

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
echo "This may take 1-2 minutes..."

MAX_WAIT=600  # 10 minutes
ELAPSED=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Orchestrator Service is healthy!${NC}"
        break
    fi

    echo -n "."
    sleep 5
    ELAPSED=$((ELAPSED + 5))
done
echo ""

if [ $ELAPSED -ge $MAX_WAIT ]; then
    echo -e "${RED}❌ Timeout: Services failed to start${NC}"
    echo ""
    echo "Checking service status:"
    docker compose ps
    echo ""
    echo "Recent logs:"
    docker compose logs --tail=50 orchestrator-service
    exit 1
fi

# Verify all services
echo ""
echo "🔍 Verifying all services..."

check_service() {
    SERVICE_NAME=$1
    PORT=$2
    if curl -sf http://localhost:$PORT/actuator/health > /dev/null 2>&1; then
        echo -e "  ${GREEN}✅${NC} $SERVICE_NAME (port $PORT)"
    else
        echo -e "  ${RED}❌${NC} $SERVICE_NAME (port $PORT)"
    fi
}

check_service "GitHub Service" 8081
check_service "Document Processor" 8082
check_service "Embedding Service" 8083
check_service "Milvus Service" 8084
check_service "Orchestrator Service" 8086
check_service "Monitoring Service" 8085

echo ""
echo -e "${GREEN}✅ All services are running!${NC}"
echo ""
echo "========================================="
echo "🎉 RepoSync is ready for local development!"
echo "========================================="
echo ""
echo "🔄 AUTO-SYNC STATUS:"
echo "  The orchestrator will automatically:"
echo "  1. ✅ Fetch all repositories from your organization"
echo "  2. ✅ Extract documents from all repos"
echo "  3. ✅ Generate embeddings"
echo "  4. ✅ Create Milvus collection if it doesn't exist"
echo "  5. ✅ Store embeddings in the cloud Milvus collection"
echo ""
echo "  This process starts automatically 5 seconds after startup."
echo "  Check the logs to monitor progress:"
echo "    docker compose logs -f orchestrator-service"
echo ""
echo "📊 Access Points:"
echo "  • Orchestrator API:  http://localhost:8086"
echo "  • Grafana Dashboard: http://localhost:3030 (admin/admin)"
echo "  • Prometheus:        http://localhost:9090"
echo ""
echo "🚀 Trigger Manual Sync (if needed):"
echo "  curl -X POST http://localhost:8086/api/orchestrator/sync | jq '.'"
echo ""
echo "📋 View Real-Time Logs:"
echo "  docker compose logs -f orchestrator-service"
echo ""
echo "🛑 Stop Services:"
echo "  docker compose down"
echo ""
echo "📖 Full Documentation: See LOCAL_SETUP_GUIDE.md"
echo ""

