#!/bin/bash

# Stop all RepoSync services

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Stopping RepoSync services...${NC}"

# Stop Docker Compose services if running
if command -v docker-compose &> /dev/null; then
    if [ -f docker-compose.yml ]; then
        echo "Stopping Docker Compose services..."
        docker-compose down
        echo -e "${GREEN}✓ Docker Compose services stopped${NC}"
    fi
fi

# Stop individual services
if [ -d logs ]; then
    for pidfile in logs/*.pid; do
        if [ -f "$pidfile" ]; then
            pid=$(cat "$pidfile")
            service_name=$(basename "$pidfile" .pid)
            if ps -p $pid > /dev/null 2>&1; then
                echo "Stopping $service_name (PID: $pid)..."
                kill $pid
                echo -e "${GREEN}✓ $service_name stopped${NC}"
            fi
            rm "$pidfile"
        fi
    done
fi

echo -e "${GREEN}All services stopped${NC}"

