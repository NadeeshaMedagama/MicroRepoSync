#!/bin/bash

# Verification Script for RepoSync Microservices Project

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "=========================================="
echo "  RepoSync Project Verification"
echo "=========================================="
echo ""

# Check function
check_exists() {
    if [ -e "$1" ]; then
        echo -e "${GREEN}✓${NC} $2"
        return 0
    else
        echo -e "${RED}✗${NC} $2"
        return 1
    fi
}

echo "Checking Project Structure..."
echo ""

# Root files
check_exists "pom.xml" "Parent POM"
check_exists "README.md" "Main README"
check_exists "QUICKSTART.md" "Quick Start Guide"
check_exists "PROJECT_STRUCTURE.md" "Project Structure Documentation"
check_exists "PROJECT_COMPLETE.md" "Project Completion Summary"
check_exists ".env.example" "Environment Template"
check_exists ".gitignore" "Git Ignore File"
check_exists "docker-compose.yml" "Docker Compose Configuration"
check_exists "start-services.sh" "Start Services Script"
check_exists "stop-services.sh" "Stop Services Script"
check_exists "deploy-k8s.sh" "Kubernetes Deployment Script"

echo ""
echo "Checking Microservices..."
echo ""

# Common Library
check_exists "common-lib/pom.xml" "Common Library POM"
check_exists "common-lib/src/main/java/com/reposync/common/dto/RepositoryInfo.java" "RepositoryInfo DTO"
check_exists "common-lib/src/main/java/com/reposync/common/dto/DocumentContent.java" "DocumentContent DTO"
check_exists "common-lib/src/main/java/com/reposync/common/dto/TextChunk.java" "TextChunk DTO"
check_exists "common-lib/src/main/java/com/reposync/common/dto/EmbeddingVector.java" "EmbeddingVector DTO"
check_exists "common-lib/src/main/java/com/reposync/common/dto/SyncJobResult.java" "SyncJobResult DTO"

echo ""

# GitHub Service
check_exists "github-service/pom.xml" "GitHub Service POM"
check_exists "github-service/Dockerfile" "GitHub Service Dockerfile"
check_exists "github-service/src/main/java/com/reposync/github/GitHubServiceApplication.java" "GitHub Service Application"
check_exists "github-service/src/main/java/com/reposync/github/controller/GitHubController.java" "GitHub Controller"
check_exists "github-service/src/main/java/com/reposync/github/service/GitHubService.java" "GitHub Service"
check_exists "github-service/src/main/resources/application.yml" "GitHub Service Config"

echo ""

# Document Processor Service
check_exists "document-processor-service/pom.xml" "Document Processor POM"
check_exists "document-processor-service/Dockerfile" "Document Processor Dockerfile"
check_exists "document-processor-service/src/main/java/com/reposync/processor/DocumentProcessorApplication.java" "Document Processor Application"
check_exists "document-processor-service/src/main/java/com/reposync/processor/controller/DocumentProcessorController.java" "Document Processor Controller"
check_exists "document-processor-service/src/main/java/com/reposync/processor/service/ChunkingService.java" "Chunking Service"
check_exists "document-processor-service/src/main/resources/application.yml" "Document Processor Config"

echo ""

# Embedding Service
check_exists "embedding-service/pom.xml" "Embedding Service POM"
check_exists "embedding-service/Dockerfile" "Embedding Service Dockerfile"
check_exists "embedding-service/src/main/java/com/reposync/embedding/EmbeddingServiceApplication.java" "Embedding Service Application"
check_exists "embedding-service/src/main/java/com/reposync/embedding/controller/EmbeddingController.java" "Embedding Controller"
check_exists "embedding-service/src/main/java/com/reposync/embedding/service/AzureOpenAIService.java" "Azure OpenAI Service"
check_exists "embedding-service/src/main/resources/application.yml" "Embedding Service Config"

echo ""

# Milvus Service
check_exists "milvus-service/pom.xml" "Milvus Service POM"
check_exists "milvus-service/Dockerfile" "Milvus Service Dockerfile"
check_exists "milvus-service/src/main/java/com/reposync/milvus/MilvusServiceApplication.java" "Milvus Service Application"
check_exists "milvus-service/src/main/java/com/reposync/milvus/controller/MilvusController.java" "Milvus Controller"
check_exists "milvus-service/src/main/java/com/reposync/milvus/service/MilvusService.java" "Milvus Service"
check_exists "milvus-service/src/main/resources/application.yml" "Milvus Service Config"

echo ""

# Orchestrator Service
check_exists "orchestrator-service/pom.xml" "Orchestrator Service POM"
check_exists "orchestrator-service/Dockerfile" "Orchestrator Service Dockerfile"
check_exists "orchestrator-service/src/main/java/com/reposync/orchestrator/OrchestratorServiceApplication.java" "Orchestrator Application"
check_exists "orchestrator-service/src/main/java/com/reposync/orchestrator/controller/OrchestratorController.java" "Orchestrator Controller"
check_exists "orchestrator-service/src/main/java/com/reposync/orchestrator/service/WorkflowOrchestrator.java" "Workflow Orchestrator"
check_exists "orchestrator-service/src/main/resources/application.yml" "Orchestrator Config"

echo ""
echo "Checking Kubernetes Manifests..."
echo ""

check_exists "k8s/01-namespace-config.yaml" "Namespace & Config"
check_exists "k8s/02-github-service.yaml" "GitHub Service K8s"
check_exists "k8s/03-document-processor-service.yaml" "Document Processor K8s"
check_exists "k8s/04-embedding-service.yaml" "Embedding Service K8s"
check_exists "k8s/05-milvus-service.yaml" "Milvus Service K8s"
check_exists "k8s/06-orchestrator-service.yaml" "Orchestrator Service K8s"

echo ""
echo "Checking GitHub Actions..."
echo ""

check_exists ".github/workflows/daily-sync.yml" "Daily Sync Workflow"
check_exists ".github/workflows/ci-cd.yml" "CI/CD Pipeline"

echo ""
echo "=========================================="
echo "  Verification Complete"
echo "=========================================="
echo ""

# Count files
java_files=$(find . -name "*.java" -type f 2>/dev/null | wc -l)
xml_files=$(find . -name "*.xml" -type f 2>/dev/null | wc -l)
yml_files=$(find . -name "*.yml" -o -name "*.yaml" -type f 2>/dev/null | wc -l)
docker_files=$(find . -name "Dockerfile" -type f 2>/dev/null | wc -l)
md_files=$(find . -name "*.md" -type f 2>/dev/null | wc -l)

echo "Project Statistics:"
echo "  Java files: $java_files"
echo "  XML files: $xml_files"
echo "  YAML files: $yml_files"
echo "  Dockerfiles: $docker_files"
echo "  Documentation: $md_files"
echo ""

echo -e "${GREEN}All checks completed!${NC}"
echo ""
echo "Next steps:"
echo "1. Install Java 17: sudo apt install openjdk-17-jdk"
echo "2. Configure .env: cp .env.example .env && nano .env"
echo "3. Build project: mvn clean install"
echo "4. Run services: ./start-services.sh"
echo ""

