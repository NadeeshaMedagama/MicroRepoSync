#!/bin/bash

# Quick Test Script for RepoSync Build
# This script verifies the build is working correctly

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=======================================${NC}"
echo -e "${GREEN} RepoSync Build Verification Test${NC}"
echo -e "${GREEN}=======================================${NC}"
echo ""

# Check Java version
echo -e "${YELLOW}Checking Java version...${NC}"
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -ge 21 ]; then
    echo -e "${GREEN}✓ Java $JAVA_VERSION detected${NC}"
else
    echo -e "${RED}✗ Java 21 or higher required. Current: $JAVA_VERSION${NC}"
    exit 1
fi
echo ""

# Check Maven
echo -e "${YELLOW}Checking Maven...${NC}"
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1)
    echo -e "${GREEN}✓ Maven detected: $MVN_VERSION${NC}"
else
    echo -e "${RED}✗ Maven not found${NC}"
    exit 1
fi
echo ""

# Run Maven build
echo -e "${YELLOW}Running Maven build...${NC}"
if mvn clean install -DskipTests -q; then
    echo -e "${GREEN}✓ Build successful!${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi
echo ""

# Check if .env file exists
echo -e "${YELLOW}Checking environment configuration...${NC}"
if [ -f .env ]; then
    echo -e "${GREEN}✓ .env file found${NC}"

    # Check for required variables
    source .env

    MISSING_VARS=()

    if [ -z "$REPOSYNC_GITHUB_TOKEN" ] || [ "$REPOSYNC_GITHUB_TOKEN" == "your_github_token_here" ]; then
        MISSING_VARS+=("REPOSYNC_GITHUB_TOKEN")
    fi

    if [ -z "$REPOSYNC_ORGANIZATION" ] || [ "$REPOSYNC_ORGANIZATION" == "your_organization_name" ]; then
        MISSING_VARS+=("REPOSYNC_ORGANIZATION")
    fi

    if [ -z "$AZURE_OPENAI_API_KEY" ] || [ "$AZURE_OPENAI_API_KEY" == "your_azure_openai_api_key" ]; then
        MISSING_VARS+=("AZURE_OPENAI_API_KEY")
    fi

    if [ -z "$AZURE_OPENAI_ENDPOINT" ] || [ "$AZURE_OPENAI_ENDPOINT" == "https://your-resource.openai.azure.com/" ]; then
        MISSING_VARS+=("AZURE_OPENAI_ENDPOINT")
    fi

    if [ ${#MISSING_VARS[@]} -gt 0 ]; then
        echo -e "${YELLOW}⚠ Warning: The following environment variables need to be configured:${NC}"
        for var in "${MISSING_VARS[@]}"; do
            echo -e "  ${RED}- $var${NC}"
        done
        echo ""
        echo -e "${YELLOW}Please edit the .env file with your actual credentials.${NC}"
    else
        echo -e "${GREEN}✓ All required environment variables are configured${NC}"
    fi
else
    echo -e "${YELLOW}⚠ .env file not found${NC}"
    echo -e "  Creating .env from .env.example..."
    cp .env.example .env
    echo -e "${RED}  Please edit .env file with your credentials!${NC}"
fi
echo ""

# Check if Docker is running
echo -e "${YELLOW}Checking Docker...${NC}"
if command -v docker &> /dev/null; then
    if docker info &> /dev/null; then
        echo -e "${GREEN}✓ Docker is running${NC}"
    else
        echo -e "${YELLOW}⚠ Docker is installed but not running${NC}"
        echo -e "  Start Docker to run services in containers"
    fi
else
    echo -e "${YELLOW}⚠ Docker not found${NC}"
    echo -e "  Install Docker to run services in containers"
fi
echo ""

# Summary
echo -e "${GREEN}=======================================${NC}"
echo -e "${GREEN} Build Verification Summary${NC}"
echo -e "${GREEN}=======================================${NC}"
echo ""
echo -e "${GREEN}All microservices built successfully!${NC}"
echo ""
echo -e "Available services:"
echo -e "  - ${GREEN}GitHub Service${NC}         (port 8081)"
echo -e "  - ${GREEN}Document Processor${NC}     (port 8082)"
echo -e "  - ${GREEN}Embedding Service${NC}      (port 8083)"
echo -e "  - ${GREEN}Milvus Service${NC}         (port 8084)"
echo -e "  - ${GREEN}Orchestrator Service${NC}   (port 8086)"
echo ""

# Next steps
echo -e "${YELLOW}Next steps:${NC}"
echo ""
echo -e "1. Configure .env file with your credentials (if not done yet)"
echo -e "   ${GREEN}nano .env${NC}"
echo ""
echo -e "2. Option A: Run with Docker Compose (recommended)"
echo -e "   ${GREEN}docker-compose up -d${NC}"
echo -e "   ${GREEN}docker-compose logs -f orchestrator-service${NC}"
echo ""
echo -e "3. Option B: Run services individually"
echo -e "   ${GREEN}# Start Milvus${NC}"
echo -e "   ${GREEN}docker-compose up -d milvus-standalone milvus-etcd milvus-minio${NC}"
echo ""
echo -e "   ${GREEN}# In separate terminals, start each service:${NC}"
echo -e "   ${GREEN}cd github-service && mvn spring-boot:run${NC}"
echo -e "   ${GREEN}cd document-processor-service && mvn spring-boot:run${NC}"
echo -e "   ${GREEN}cd embedding-service && mvn spring-boot:run${NC}"
echo -e "   ${GREEN}cd milvus-service && mvn spring-boot:run${NC}"
echo -e "   ${GREEN}cd orchestrator-service && mvn spring-boot:run${NC}"
echo ""
echo -e "4. Trigger a sync manually:"
echo -e "   ${GREEN}curl -X POST http://localhost:8086/api/orchestrator/sync${NC}"
echo ""
echo -e "5. Check service health:"
echo -e "   ${GREEN}curl http://localhost:8086/actuator/health${NC}"
echo ""
echo -e "${GREEN}=======================================${NC}"
echo ""

