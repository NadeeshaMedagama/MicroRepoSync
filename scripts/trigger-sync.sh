#!/bin/bash

# ============================================
# Manual Sync Trigger Script
# ============================================
# This script triggers a manual sync to update the Milvus collection

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo "🔄 Manual RepoSync Trigger"
echo "=========================="
echo ""

# Check if orchestrator is running
echo "Checking if Orchestrator Service is available..."
if ! curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${RED}❌ Orchestrator Service is not running${NC}"
    echo ""
    echo "Please start the services first:"
    echo "  ./scripts/start-local.sh"
    echo "  or"
    echo "  docker compose up -d"
    exit 1
fi
echo -e "${GREEN}✅ Orchestrator Service is running${NC}"
echo ""

# Show current configuration
echo "📋 Current Configuration:"
source .env 2>/dev/null || true
echo "  Organization: ${REPOSYNC_ORGANIZATION:-All}"
echo "  Filter Keyword: ${REPOSYNC_FILTER_KEYWORD:-None}"
echo "  Collection: ${MILVUS_COLLECTION_NAME:-reposync_collection}"
echo ""

# Ask for confirmation
read -p "Trigger sync now? (y/N) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Sync cancelled."
    exit 0
fi

echo ""
echo "🚀 Triggering sync..."
echo "This may take several minutes depending on the number of repositories..."
echo ""

# Trigger sync
RESPONSE_FILE=$(mktemp)
HTTP_CODE=$(curl -s -w "%{http_code}" -X POST http://localhost:8080/api/orchestrator/sync \
    -H "Content-Type: application/json" \
    -o "$RESPONSE_FILE")

echo "HTTP Response Code: $HTTP_CODE"
echo ""

# Check response
if [ "$HTTP_CODE" != "200" ]; then
    echo -e "${RED}❌ Sync request failed!${NC}"
    echo "Response:"
    cat "$RESPONSE_FILE"
    rm "$RESPONSE_FILE"
    exit 1
fi

# Parse and display result
echo "📊 Sync Result:"
echo "==============="
cat "$RESPONSE_FILE" | jq '.' || cat "$RESPONSE_FILE"
echo ""

# Extract status
STATUS=$(cat "$RESPONSE_FILE" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")

if [ "$STATUS" = "SUCCESS" ]; then
    echo -e "${GREEN}✅ Sync completed successfully!${NC}"
    echo ""
    echo "Statistics:"
    cat "$RESPONSE_FILE" | jq -r '
        "  Repositories: \(.repositoriesProcessed // 0)",
        "  Documents:    \(.documentsProcessed // 0)",
        "  Chunks:       \(.chunksCreated // 0)",
        "  Vectors:      \(.vectorsStored // 0)"
    ' 2>/dev/null || echo "  (Statistics not available)"
elif [ "$STATUS" = "IN_PROGRESS" ]; then
    echo -e "${YELLOW}⏳ Sync is in progress...${NC}"
    echo ""
    echo "Monitor progress:"
    echo "  docker compose logs -f orchestrator-service"
elif [ "$STATUS" = "FAILED" ]; then
    echo -e "${RED}❌ Sync failed!${NC}"
    ERROR_MSG=$(cat "$RESPONSE_FILE" | jq -r '.errorMessage' 2>/dev/null || echo "Unknown error")
    echo "Error: $ERROR_MSG"
    echo ""
    echo "Check logs for details:"
    echo "  docker compose logs orchestrator-service"
else
    echo -e "${YELLOW}⚠️  Unknown status: $STATUS${NC}"
fi

rm "$RESPONSE_FILE"

echo ""
echo "🔍 Next Steps:"
echo "  • View logs: docker compose logs -f orchestrator-service"
echo "  • Check Milvus stats: curl http://localhost:8084/api/milvus/stats"
echo "  • Open Grafana: http://localhost:3000"
echo ""

