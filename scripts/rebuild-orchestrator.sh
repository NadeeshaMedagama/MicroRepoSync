#!/bin/bash

# ============================================
# Quick Rebuild & Restart Orchestrator
# ============================================
# Use this script when you've made changes to
# the orchestrator service and want to test them

set -e

echo "🔄 Rebuilding Orchestrator Service"
echo "===================================="
echo ""

cd "$(dirname "$0")/.."

# Build orchestrator service
echo "📦 Building orchestrator-service..."
echo "(Ignore any Log4j2 warnings - they are harmless)"
mvn clean package -pl orchestrator-service -am -DskipTests -Dcheckstyle.skip=true -q \
    -Dorg.slf4j.simpleLogger.defaultLogLevel=WARN \
    -Dlog4j2.statusLoggerLevel=OFF 2>&1 | grep -v "StatusLogger" || true

if [ ${PIPESTATUS[0]} -eq 0 ]; then
    echo "✅ Build successful"
else
    echo "❌ Build failed"
    exit 1
fi
echo ""

# Stop the existing container first
echo "🛑 Stopping existing orchestrator-service..."
docker compose stop orchestrator-service
docker compose rm -f orchestrator-service

echo ""

# Rebuild Docker image
echo "🐳 Rebuilding Docker image..."
docker compose build orchestrator-service

echo ""

# Start the new service
echo "🚀 Starting orchestrator-service..."
docker compose up -d orchestrator-service

echo ""
echo "⏳ Waiting for service to become healthy..."
sleep 10

# Check health
if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ Orchestrator is healthy!"
    echo ""
    echo "📋 Monitoring auto-sync..."
    echo "Press Ctrl+C to stop watching logs"
    echo ""
    sleep 2
    docker compose logs -f orchestrator-service
else
    echo "⚠️  Service may still be starting..."
    echo "Check logs with: docker compose logs -f orchestrator-service"
fi

