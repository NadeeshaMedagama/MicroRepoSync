#!/bin/bash

# ============================================
# Check Milvus Collection Status
# ============================================
# This script displays the current status of your Milvus collection

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo "📊 Milvus Collection Status"
echo "============================"
echo ""

# Check if Milvus service is running
if ! curl -sf http://localhost:8084/actuator/health > /dev/null 2>&1; then
    echo -e "${RED}❌ Milvus Service is not running${NC}"
    echo ""
    echo "Please start the services first:"
    echo "  ./scripts/start-local.sh"
    exit 1
fi

echo -e "${GREEN}✅ Milvus Service is running${NC}"
echo ""

# Get collection name from env
source .env 2>/dev/null || true
COLLECTION_NAME=${MILVUS_COLLECTION_NAME:-reposync_collection}

echo "Collection: $COLLECTION_NAME"
echo ""

# Try to get collection stats
echo "Fetching collection statistics..."
echo ""

# Check if stats endpoint exists
STATS_RESPONSE=$(curl -s http://localhost:8084/api/milvus/stats 2>/dev/null)

if [ $? -eq 0 ] && [ ! -z "$STATS_RESPONSE" ]; then
    echo "📈 Statistics:"
    echo "$STATS_RESPONSE" | jq '.' 2>/dev/null || echo "$STATS_RESPONSE"
else
    echo -e "${YELLOW}⚠️  Stats endpoint not available or collection is empty${NC}"
fi

echo ""
echo "🔍 Service Health Details:"
HEALTH_RESPONSE=$(curl -s http://localhost:8084/actuator/health)
echo "$HEALTH_RESPONSE" | jq '.' 2>/dev/null || echo "$HEALTH_RESPONSE"

echo ""
echo "📋 Milvus Container Status:"
docker compose ps milvus-standalone milvus-service

echo ""
echo "🛠️  Available Actions:"
echo "  • Trigger new sync:     ./scripts/trigger-sync.sh"
echo "  • View Milvus logs:     docker compose logs milvus-service"
echo "  • View standalone logs: docker compose logs milvus-standalone"
echo "  • Clear collection:     docker compose down -v (WARNING: Deletes all data!)"
echo ""

