#!/bin/bash

# ============================================
# Quick Fix for Port 8086 Already Allocated
# ============================================

set -e

echo "🔧 Fixing Port Conflict Issue"
echo "======================================"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

cd "$(dirname "$0")/.."

echo "1️⃣ Stopping all containers..."
docker compose down 2>/dev/null || true

echo ""
echo "2️⃣ Checking if port 8086 is still in use..."
if lsof -i :8086 > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Port 8086 is still in use${NC}"
    echo "Finding process..."
    PID=$(lsof -ti:8086 || true)
    if [ -n "$PID" ]; then
        echo "Process using port 8086: PID $PID"
        echo "Attempting to kill process..."
        kill -9 $PID 2>/dev/null || sudo kill -9 $PID 2>/dev/null || true
        sleep 2
    fi
else
    echo -e "${GREEN}✅ Port 8086 is free${NC}"
fi

echo ""
echo "3️⃣ Cleaning up Docker resources..."
docker system prune -f > /dev/null 2>&1 || true

echo ""
echo "4️⃣ Starting services..."
docker compose up -d

echo ""
echo "5️⃣ Waiting for services to be healthy..."
sleep 10

# Check service status
if curl -sf http://localhost:8086/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Orchestrator is healthy!${NC}"
    echo ""
    echo "🎉 All fixed! Services are running."
    echo ""
    echo "Next steps:"
    echo "  • Verify auto-sync: ./scripts/verify-auto-sync.sh"
    echo "  • Watch logs: docker compose logs -f orchestrator-service"
else
    echo -e "${YELLOW}⚠️  Services may still be starting...${NC}"
    echo "Check status with: docker compose ps"
    echo "View logs with: docker compose logs orchestrator-service"
fi

