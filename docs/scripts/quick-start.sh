#!/bin/bash

# RepoSync Microservices - Quick Start Script
# This script helps you quickly set up and run the project locally

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  RepoSync Microservices - Quick Start${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""

# Function to check prerequisites
check_prerequisites() {
    echo -e "${BLUE}Checking prerequisites...${NC}"

    # Check Java
    if ! command -v java &> /dev/null; then
        echo -e "${RED}✗ Java is not installed${NC}"
        echo "  Please install Java 21 or higher"
        exit 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "?\K[0-9]+' | head -1)
    if [ "$JAVA_VERSION" -lt 21 ]; then
        echo -e "${RED}✗ Java version must be 21 or higher (found: $JAVA_VERSION)${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Java $JAVA_VERSION detected${NC}"

    # Check Maven
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}✗ Maven is not installed${NC}"
        echo "  Please install Maven 3.6 or higher"
        exit 1
    fi
    echo -e "${GREEN}✓ Maven detected${NC}"

    # Check Docker (optional for some run modes)
    if command -v docker &> /dev/null; then
        echo -e "${GREEN}✓ Docker detected${NC}"
    else
        echo -e "${YELLOW}⚠ Docker not found (needed for Option 1 and Milvus)${NC}"
    fi

    echo ""
}

# Function to check .env file
check_env_file() {
    echo -e "${BLUE}Checking environment configuration...${NC}"

    if [ ! -f .env ]; then
        echo -e "${YELLOW}⚠ .env file not found${NC}"
        echo "Creating .env from .env.example..."
        cp .env.example .env
        echo -e "${RED}✗ Please edit .env file with your credentials!${NC}"
        echo ""
        echo "Required variables:"
        echo "  - REPOSYNC_GITHUB_TOKEN"
        echo "  - REPOSYNC_ORGANIZATION"
        echo "  - AZURE_OPENAI_API_KEY"
        echo "  - AZURE_OPENAI_ENDPOINT"
        echo "  - AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT"
        echo ""
        echo "Run this script again after configuring .env"
        exit 1
    fi

    # Check if .env has been configured (not using example values)
    if grep -q "your_github_token_here" .env || grep -q "your_azure_openai_api_key" .env; then
        echo -e "${RED}✗ .env file contains example values${NC}"
        echo "  Please edit .env with your actual credentials"
        exit 1
    fi

    echo -e "${GREEN}✓ .env file configured${NC}"
    echo ""
}

# Function to build project
build_project() {
    echo -e "${BLUE}Building project...${NC}"
    echo "This may take a few minutes on first run..."
    echo ""

    mvn clean install -DskipTests

    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✓ Build successful${NC}"
        echo ""
    else
        echo -e "${RED}✗ Build failed${NC}"
        echo "  Check the error messages above"
        exit 1
    fi
}

# Function to show menu
show_menu() {
    echo -e "${BLUE}How would you like to run the services?${NC}"
    echo ""
    echo "1) Docker Compose (Recommended - All services in containers)"
    echo "2) Individual Services (For development - Run each service separately)"
    echo "3) Just build the project (Don't start services)"
    echo "4) Run tests only"
    echo "5) Exit"
    echo ""
    read -p "Enter your choice [1-5]: " choice
    echo ""
}

# Function to run with Docker Compose
run_docker_compose() {
    echo -e "${BLUE}Starting services with Docker Compose...${NC}"

    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        echo -e "${RED}✗ Docker Compose is not installed${NC}"
        exit 1
    fi

    # Load environment variables
    export $(cat .env | grep -v '^#' | xargs)

    # Start services
    if docker compose version &> /dev/null; then
        docker compose up -d --build
    else
        docker-compose up -d --build
    fi

    echo ""
    echo -e "${GREEN}✓ Services starting...${NC}"
    echo ""
    echo "Waiting for services to be healthy (this may take 1-2 minutes)..."
    sleep 30

    echo ""
    echo -e "${GREEN}Services status:${NC}"
    if docker compose version &> /dev/null; then
        docker compose ps
    else
        docker-compose ps
    fi

    echo ""
    echo -e "${GREEN}✓ Services are running!${NC}"
    echo ""
    echo "Access points:"
    echo "  - Orchestrator Service: http://localhost:8086"
    echo "  - GitHub Service: http://localhost:8081"
    echo "  - Document Processor: http://localhost:8082"
    echo "  - Embedding Service: http://localhost:8083"
    echo "  - Milvus Service: http://localhost:8084"
    echo ""
    echo "To trigger a sync: curl -X POST http://localhost:8086/api/orchestrator/sync"
    echo ""
    echo "View logs: docker-compose logs -f"
    echo "Stop services: docker-compose down"
}

# Function to run individual services
run_individual_services() {
    echo -e "${BLUE}Running individual services...${NC}"
    echo ""
    echo -e "${YELLOW}You need to open 5 separate terminal windows.${NC}"
    echo ""
    echo "First, start Milvus in Docker:"
    echo -e "${GREEN}docker run -d --name milvus-standalone -p 19530:19530 milvusdb/milvus:latest milvus run standalone${NC}"
    echo ""
    echo "Then, in separate terminals, run:"
    echo ""
    echo -e "${GREEN}Terminal 1:${NC}"
    echo "cd github-service && export \$(cat ../.env | grep -v '^#' | xargs) && mvn spring-boot:run"
    echo ""
    echo -e "${GREEN}Terminal 2:${NC}"
    echo "cd document-processor-service && mvn spring-boot:run"
    echo ""
    echo -e "${GREEN}Terminal 3:${NC}"
    echo "cd embedding-service && export \$(cat ../.env | grep -v '^#' | xargs) && mvn spring-boot:run"
    echo ""
    echo -e "${GREEN}Terminal 4:${NC}"
    echo "cd milvus-service && export \$(cat ../.env | grep -v '^#' | xargs) && mvn spring-boot:run"
    echo ""
    echo -e "${GREEN}Terminal 5:${NC}"
    echo "cd orchestrator-service && export \$(cat ../.env | grep -v '^#' | xargs) && mvn spring-boot:run"
    echo ""
}

# Function to run tests
run_tests() {
    echo -e "${BLUE}Running tests...${NC}"
    mvn test

    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✓ All tests passed${NC}"
    else
        echo -e "${RED}✗ Some tests failed${NC}"
        exit 1
    fi
}

# Main execution
main() {
    check_prerequisites
    check_env_file

    echo -e "${BLUE}Do you want to build the project first?${NC}"
    read -p "Build now? [Y/n]: " build_choice
    echo ""

    if [[ "$build_choice" != "n" && "$build_choice" != "N" ]]; then
        build_project
    fi

    show_menu

    case $choice in
        1)
            run_docker_compose
            ;;
        2)
            run_individual_services
            ;;
        3)
            echo -e "${GREEN}Build completed. Services not started.${NC}"
            ;;
        4)
            run_tests
            ;;
        5)
            echo -e "${GREEN}Goodbye!${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid choice${NC}"
            exit 1
            ;;
    esac
}

# Run main function
main

