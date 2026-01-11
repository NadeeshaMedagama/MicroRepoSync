#!/bin/bash

# Kubernetes Deployment Script for RepoSync

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  RepoSync Kubernetes Deployment${NC}"
echo -e "${GREEN}========================================${NC}"

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}kubectl is not installed. Please install kubectl first.${NC}"
    exit 1
fi

# Check if .env file exists
if [ ! -f .env ]; then
    echo -e "${RED}.env file not found. Please create it from .env.example${NC}"
    exit 1
fi

# Load environment variables
export $(cat .env | grep -v '^#' | xargs)

echo -e "\n${YELLOW}Step 1: Creating namespace and basic configuration${NC}"
kubectl apply -f k8s/01-namespace-config.yaml

echo -e "\n${YELLOW}Step 2: Updating secrets${NC}"
kubectl create secret generic reposync-secrets \
  --from-literal=REPOSYNC_GITHUB_TOKEN="${REPOSYNC_GITHUB_TOKEN}" \
  --from-literal=AZURE_OPENAI_API_KEY="${AZURE_OPENAI_API_KEY}" \
  --from-literal=MILVUS_TOKEN="${MILVUS_TOKEN}" \
  --namespace=reposync \
  --dry-run=client -o yaml | kubectl apply -f -

echo -e "\n${YELLOW}Step 3: Updating ConfigMap${NC}"
kubectl create configmap reposync-config \
  --from-literal=REPOSYNC_ORGANIZATION="${REPOSYNC_ORGANIZATION}" \
  --from-literal=REPOSYNC_FILTER_KEYWORD="${REPOSYNC_FILTER_KEYWORD}" \
  --from-literal=AZURE_OPENAI_ENDPOINT="${AZURE_OPENAI_ENDPOINT}" \
  --from-literal=AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT="${AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT}" \
  --from-literal=MILVUS_URI="${MILVUS_URI}" \
  --from-literal=MILVUS_COLLECTION_NAME="${MILVUS_COLLECTION_NAME}" \
  --from-literal=GITHUB_SERVICE_URL="http://github-service:8081" \
  --from-literal=PROCESSOR_SERVICE_URL="http://document-processor-service:8082" \
  --from-literal=EMBEDDING_SERVICE_URL="http://embedding-service:8083" \
  --from-literal=MILVUS_SERVICE_URL="http://milvus-service:8084" \
  --namespace=reposync \
  --dry-run=client -o yaml | kubectl apply -f -

echo -e "\n${YELLOW}Step 4: Deploying services${NC}"
kubectl apply -f k8s/02-github-service.yaml
kubectl apply -f k8s/03-document-processor-service.yaml
kubectl apply -f k8s/04-embedding-service.yaml
kubectl apply -f k8s/05-milvus-service.yaml
kubectl apply -f k8s/06-orchestrator-service.yaml

echo -e "\n${YELLOW}Step 5: Waiting for deployments to be ready${NC}"
kubectl rollout status deployment/github-service -n reposync --timeout=5m
kubectl rollout status deployment/document-processor-service -n reposync --timeout=5m
kubectl rollout status deployment/embedding-service -n reposync --timeout=5m
kubectl rollout status deployment/milvus-service -n reposync --timeout=5m
kubectl rollout status deployment/orchestrator-service -n reposync --timeout=5m

echo -e "\n${GREEN}✓ Deployment completed successfully!${NC}"

echo -e "\n${YELLOW}Deployment Status:${NC}"
kubectl get pods -n reposync
kubectl get services -n reposync

echo -e "\n${YELLOW}To check logs:${NC}"
echo "kubectl logs -f deployment/orchestrator-service -n reposync"

echo -e "\n${YELLOW}To trigger sync:${NC}"
echo "kubectl exec -it deployment/orchestrator-service -n reposync -- curl -X POST http://localhost:8080/api/orchestrator/sync"

echo -e "\n${GREEN}Deployment complete!${NC}"

