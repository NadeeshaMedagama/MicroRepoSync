#!/bin/bash

# ============================================
# RepoSync Auto-Sync Verification Script
# ============================================

set -e

echo "🔍 RepoSync Auto-Sync Verification"
echo "===================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check if services are running
echo "1️⃣ Checking if services are running..."
if docker compose ps | grep -q "Up"; then
    echo -e "${GREEN}✅ Docker Compose services are running${NC}"
else
    echo -e "${RED}❌ Services are not running. Start them with: ./scripts/start-local.sh${NC}"
    exit 1
fi
echo ""

# Check orchestrator health
echo "2️⃣ Checking orchestrator service health..."
if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Orchestrator is healthy${NC}"
    if command -v jq &> /dev/null; then
        curl -s http://localhost:8080/actuator/health | jq '.'
    else
        curl -s http://localhost:8080/actuator/health
    fi
else
    echo -e "${RED}❌ Orchestrator is not responding${NC}"
    echo "Checking logs..."
    docker compose logs --tail=50 orchestrator-service
    exit 1
fi
echo ""

# Check if auto-sync was triggered
echo "3️⃣ Checking if auto-sync was triggered..."
if docker compose logs orchestrator-service | grep -q "Auto-triggering initial sync workflow"; then
    echo -e "${GREEN}✅ Auto-sync was triggered on startup${NC}"
else
    echo -e "${YELLOW}⚠️  Auto-sync trigger not found in logs${NC}"
    echo "This might mean:"
    echo "  - Services just started (wait 10 seconds)"
    echo "  - Auto-sync is disabled (check REPOSYNC_AUTO_SYNC_ON_STARTUP)"
fi
echo ""

# Check if sync completed
echo "4️⃣ Checking sync completion status..."
if docker compose logs orchestrator-service | grep -q "Sync workflow completed successfully"; then
    echo -e "${GREEN}✅ Sync workflow completed successfully!${NC}"

    # Show summary
    echo ""
    echo "📊 Sync Summary:"
    docker compose logs orchestrator-service | grep -E "repositories|documents|chunks|embeddings|vectors" | tail -10

elif docker compose logs orchestrator-service | grep -q "Sync workflow FAILED"; then
    echo -e "${RED}❌ Sync workflow failed${NC}"
    echo ""
    echo "Error details:"
    docker compose logs orchestrator-service | grep -E "FAILED|Error|Exception" | tail -20
else
    echo -e "${YELLOW}⚠️  Sync is still in progress or hasn't started yet${NC}"
    echo ""
    echo "Recent orchestrator logs:"
    docker compose logs orchestrator-service | tail -30
fi
echo ""

# Check Milvus collection
echo "5️⃣ Checking Milvus collection status..."
if docker compose logs milvus-service | grep -q "Collection"; then
    echo -e "${GREEN}✅ Milvus collection operations found${NC}"
    docker compose logs milvus-service | grep "Collection" | tail -5
else
    echo -e "${YELLOW}⚠️  No collection operations found yet${NC}"
fi
echo ""

# Service health summary
echo "6️⃣ Service Health Summary:"
echo ""
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
check_service "Orchestrator Service" 8080
check_service "Monitoring Service" 8085

echo ""
echo "========================================="
echo "📋 Quick Commands:"
echo "========================================="
echo ""
echo "View live orchestrator logs:"
echo "  docker compose logs -f orchestrator-service"
echo ""
echo "Manually trigger sync:"
if command -v jq &> /dev/null; then
    echo "  curl -X POST http://localhost:8080/api/orchestrator/sync | jq '.'"
else
    echo "  curl -X POST http://localhost:8080/api/orchestrator/sync"
fi
echo ""
echo "Check all service logs:"
echo "  docker compose logs -f"
echo ""
echo "Restart orchestrator to trigger auto-sync again:"
echo "  docker compose restart orchestrator-service"
echo ""

