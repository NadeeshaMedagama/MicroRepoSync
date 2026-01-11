#!/bin/bash

# RepoSync Monitoring Quick Start Script
# This script helps you quickly start and verify the monitoring system

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}RepoSync Monitoring System Quick Start${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
        return 1
    fi
}

# Function to wait for service
wait_for_service() {
    local service_name=$1
    local url=$2
    local max_attempts=30
    local attempt=1

    echo -e "${YELLOW}Waiting for $service_name to be ready...${NC}"

    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "$url" > /dev/null 2>&1; then
            print_status 0 "$service_name is ready"
            return 0
        fi
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done

    print_status 1 "$service_name failed to start"
    return 1
}

# Check if Docker is running
echo -e "${YELLOW}Checking prerequisites...${NC}"
if ! docker info > /dev/null 2>&1; then
    print_status 1 "Docker is not running. Please start Docker first."
    exit 1
fi
print_status 0 "Docker is running"

# Check if docker-compose is installed
if ! command -v docker-compose &> /dev/null; then
    print_status 1 "docker-compose is not installed. Please install docker-compose first."
    exit 1
fi
print_status 0 "docker-compose is installed"

# Build the monitoring service
echo ""
echo -e "${YELLOW}Building monitoring service...${NC}"
if mvn clean package -DskipTests -Dcheckstyle.skip=true -pl monitoring-service -am > /dev/null 2>&1; then
    print_status 0 "Monitoring service built successfully"
else
    print_status 1 "Failed to build monitoring service"
    echo -e "${RED}Build error detected. Trying with verbose output...${NC}"
    mvn clean package -DskipTests -Dcheckstyle.skip=true -pl monitoring-service -am
    exit 1
fi

# Start monitoring stack
echo ""
echo -e "${YELLOW}Starting monitoring stack...${NC}"
docker-compose up -d monitoring-service prometheus grafana

# Wait for services to be ready
echo ""
echo -e "${YELLOW}Waiting for services to be ready...${NC}"
wait_for_service "Monitoring Service" "http://localhost:8085/actuator/health" || exit 1
wait_for_service "Prometheus" "http://localhost:9090/-/healthy" || exit 1
wait_for_service "Grafana" "http://localhost:3000/api/health" || exit 1

# Verify Prometheus targets
echo ""
echo -e "${YELLOW}Checking Prometheus targets...${NC}"
sleep 5
if curl -s "http://localhost:9090/api/v1/targets" | grep -q "monitoring-service"; then
    print_status 0 "Prometheus is scraping monitoring service"
else
    print_status 1 "Prometheus is not configured correctly"
fi

# Print access information
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Monitoring System Started Successfully!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}Access URLs:${NC}"
echo -e "  • Prometheus:         ${YELLOW}http://localhost:9090${NC}"
echo -e "  • Grafana:            ${YELLOW}http://localhost:3000${NC} (admin/admin)"
echo -e "  • Monitoring API:     ${YELLOW}http://localhost:8085/api/monitoring${NC}"
echo ""
echo -e "${BLUE}Useful Commands:${NC}"
echo -e "  • View system health: ${YELLOW}curl http://localhost:8085/api/monitoring/health${NC}"
echo -e "  • View all services:  ${YELLOW}curl http://localhost:8085/api/monitoring/services/health${NC}"
echo -e "  • View logs:          ${YELLOW}docker-compose logs -f monitoring-service${NC}"
echo -e "  • Stop monitoring:    ${YELLOW}docker-compose stop monitoring-service prometheus grafana${NC}"
echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo -e "  1. Open Grafana at http://localhost:3000"
echo -e "  2. Login with admin/admin"
echo -e "  3. Import the RepoSync dashboard from monitoring/grafana/dashboards/"
echo -e "  4. Explore Prometheus at http://localhost:9090"
echo ""
echo -e "${GREEN}For more information, see docs/readmes/MONITORING_GUIDE.md${NC}"
echo ""

