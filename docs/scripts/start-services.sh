#!/bin/bash

# RepoSync Local Development Helper Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  RepoSync Microservices Setup${NC}"
echo -e "${GREEN}========================================${NC}"

# Check if .env file exists
if [ ! -f .env ]; then
    echo -e "${YELLOW}Warning: .env file not found!${NC}"
    echo "Creating .env from .env.example..."
    cp .env.example .env
    echo -e "${RED}Please edit .env file with your credentials before proceeding!${NC}"
    exit 1
fi

# Function to check if Java is installed
check_java() {
    if ! command -v java &> /dev/null; then
        echo -e "${RED}Java is not installed. Please install Java 17 or higher.${NC}"
        exit 1
    fi
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        echo -e "${RED}Java version must be 17 or higher. Current version: $JAVA_VERSION${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Java $JAVA_VERSION detected${NC}"
}

# Function to check if Maven is installed
check_maven() {
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}Maven is not installed. Please install Maven 3.6 or higher.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Maven detected${NC}"
}

# Function to check if Docker is installed
check_docker() {
    if ! command -v docker &> /dev/null; then
        echo -e "${YELLOW}Warning: Docker is not installed. Docker Compose mode will not be available.${NC}"
        return 1
    fi
    echo -e "${GREEN}✓ Docker detected${NC}"
    return 0
}

# Function to build the project
build_project() {
    echo -e "\n${YELLOW}Building all services...${NC}"
    mvn clean install -DskipTests
    echo -e "${GREEN}✓ Build completed successfully${NC}"
}

# Function to run with Docker Compose
run_docker_compose() {
    echo -e "\n${YELLOW}Starting services with Docker Compose...${NC}"
    docker-compose up -d
    echo -e "${GREEN}✓ All services started${NC}"
    echo -e "\nWaiting for services to be ready..."
    sleep 45
    echo -e "\n${GREEN}Services Status:${NC}"
    docker-compose ps
    echo -e "\n${GREEN}To view logs: docker-compose logs -f${NC}"
    echo -e "${GREEN}To stop services: docker-compose down${NC}"
}

# Function to run services individually
run_individual() {
    echo -e "\n${YELLOW}Starting services individually...${NC}"
    echo "This will open services in background processes."

    # Load environment variables
    export $(cat .env | xargs)

    # Start services
    echo "Starting GitHub Service..."
    cd github-service && nohup mvn spring-boot:run > ../logs/github-service.log 2>&1 & echo $! > ../logs/github-service.pid
    cd ..

    sleep 10

    echo "Starting Document Processor Service..."
    cd document-processor-service && nohup mvn spring-boot:run > ../logs/processor-service.log 2>&1 & echo $! > ../logs/processor-service.pid
    cd ..

    sleep 10

    echo "Starting Embedding Service..."
    cd embedding-service && nohup mvn spring-boot:run > ../logs/embedding-service.log 2>&1 & echo $! > ../logs/embedding-service.pid
    cd ..

    sleep 10

    echo "Starting Milvus Service..."
    cd milvus-service && nohup mvn spring-boot:run > ../logs/milvus-service.log 2>&1 & echo $! > ../logs/milvus-service.pid
    cd ..

    sleep 10

    echo "Starting Orchestrator Service..."
    cd orchestrator-service && nohup mvn spring-boot:run > ../logs/orchestrator-service.log 2>&1 & echo $! > ../logs/orchestrator-service.pid
    cd ..

    echo -e "${GREEN}✓ All services started${NC}"
    echo -e "\nLogs are available in the 'logs' directory"
    echo -e "To stop services, run: ./stop-services.sh"
}

# Function to trigger sync
trigger_sync() {
    echo -e "\n${YELLOW}Triggering sync job...${NC}"
    sleep 30  # Wait for services to be fully ready
    curl -X POST http://localhost:8080/api/orchestrator/sync -H "Content-Type: application/json"
    echo -e "\n${GREEN}✓ Sync triggered${NC}"
}

# Main menu
main() {
    check_java
    check_maven

    echo -e "\n${YELLOW}What would you like to do?${NC}"
    echo "1) Build project"
    echo "2) Run with Docker Compose (requires Docker)"
    echo "3) Run services individually (in background)"
    echo "4) Build and run with Docker Compose"
    echo "5) Build and run individually"
    echo "6) Trigger sync job"
    echo "7) Exit"

    read -p "Enter your choice [1-7]: " choice

    case $choice in
        1)
            build_project
            ;;
        2)
            if check_docker; then
                run_docker_compose
            else
                echo -e "${RED}Docker is not available${NC}"
                exit 1
            fi
            ;;
        3)
            mkdir -p logs
            run_individual
            ;;
        4)
            if check_docker; then
                build_project
                run_docker_compose
            else
                echo -e "${RED}Docker is not available${NC}"
                exit 1
            fi
            ;;
        5)
            build_project
            mkdir -p logs
            run_individual
            ;;
        6)
            trigger_sync
            ;;
        7)
            echo "Goodbye!"
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid choice${NC}"
            exit 1
            ;;
    esac
}

main

