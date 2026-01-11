#!/bin/bash

# Monitoring System Verification Script
# Verifies that all monitoring components are properly configured

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Monitoring System Verification${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Counters
PASSED=0
FAILED=0

# Function to check file exists
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $2"
        ((PASSED++))
    else
        echo -e "${RED}✗${NC} $2 - NOT FOUND: $1"
        ((FAILED++))
    fi
}

# Function to check directory exists
check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} $2"
        ((PASSED++))
    else
        echo -e "${RED}✗${NC} $2 - NOT FOUND: $1"
        ((FAILED++))
    fi
}

echo -e "${YELLOW}Checking Monitoring Service...${NC}"
check_dir "monitoring-service" "Monitoring service directory"
check_file "monitoring-service/pom.xml" "Monitoring service POM"
check_file "monitoring-service/Dockerfile" "Monitoring service Dockerfile"
check_file "monitoring-service/src/main/java/com/reposync/monitoring/MonitoringServiceApplication.java" "Main application class"
check_file "monitoring-service/src/main/resources/application.yml" "Application configuration"

echo ""
echo -e "${YELLOW}Checking Service Implementations...${NC}"
check_file "monitoring-service/src/main/java/com/reposync/monitoring/service/HealthCheckService.java" "HealthCheckService interface"
check_file "monitoring-service/src/main/java/com/reposync/monitoring/service/HealthCheckServiceImpl.java" "HealthCheckService implementation"
check_file "monitoring-service/src/main/java/com/reposync/monitoring/service/MetricsCollectionService.java" "MetricsCollectionService interface"
check_file "monitoring-service/src/main/java/com/reposync/monitoring/service/MetricsCollectionServiceImpl.java" "MetricsCollectionService implementation"
check_file "monitoring-service/src/main/java/com/reposync/monitoring/service/MonitoringService.java" "MonitoringService"

echo ""
echo -e "${YELLOW}Checking Controllers and Models...${NC}"
check_file "monitoring-service/src/main/java/com/reposync/monitoring/controller/MonitoringController.java" "Monitoring controller"
check_file "monitoring-service/src/main/java/com/reposync/monitoring/model/ServiceHealth.java" "ServiceHealth model"
check_file "monitoring-service/src/main/java/com/reposync/monitoring/model/ServiceMetrics.java" "ServiceMetrics model"

echo ""
echo -e "${YELLOW}Checking Prometheus Configuration...${NC}"
check_dir "monitoring/prometheus" "Prometheus directory"
check_file "monitoring/prometheus/prometheus.yml" "Prometheus configuration"
check_dir "monitoring/prometheus/rules" "Prometheus rules directory"
check_file "monitoring/prometheus/rules/alerts.yml" "Alert rules"

echo ""
echo -e "${YELLOW}Checking Grafana Configuration...${NC}"
check_dir "monitoring/grafana" "Grafana directory"
check_dir "monitoring/grafana/dashboards" "Grafana dashboards directory"
check_file "monitoring/grafana/dashboards/reposync-overview.json" "Main dashboard"
check_dir "monitoring/grafana/provisioning/datasources" "Datasources directory"
check_file "monitoring/grafana/provisioning/datasources/prometheus.yml" "Prometheus datasource"
check_file "monitoring/grafana/provisioning/dashboards/dashboard.yml" "Dashboard provisioning"

echo ""
echo -e "${YELLOW}Checking Kubernetes Manifests...${NC}"
check_file "k8s/07-monitoring-service.yaml" "Monitoring service K8s manifest"
check_file "k8s/07-prometheus.yaml" "Prometheus K8s manifest"
check_file "k8s/08-grafana.yaml" "Grafana K8s manifest"

echo ""
echo -e "${YELLOW}Checking Documentation...${NC}"
check_file "docs/readmes/MONITORING_GUIDE.md" "Monitoring guide"
check_file "docs/readmes/MONITORING_QUICKSTART.md" "Quick start guide"
check_file "docs/readmes/MONITORING_IMPLEMENTATION_SUMMARY.md" "Implementation summary"
check_file "docs/scripts/start-monitoring.sh" "Start monitoring script"

echo ""
echo -e "${YELLOW}Checking Docker Configuration...${NC}"
check_file "docker-compose.yml" "Docker Compose file"

# Check if monitoring-service is in docker-compose.yml
if grep -q "monitoring-service:" docker-compose.yml; then
    echo -e "${GREEN}✓${NC} Monitoring service in docker-compose.yml"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} Monitoring service NOT in docker-compose.yml"
    ((FAILED++))
fi

# Check if prometheus is in docker-compose.yml
if grep -q "prometheus:" docker-compose.yml; then
    echo -e "${GREEN}✓${NC} Prometheus in docker-compose.yml"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} Prometheus NOT in docker-compose.yml"
    ((FAILED++))
fi

# Check if grafana is in docker-compose.yml
if grep -q "grafana:" docker-compose.yml; then
    echo -e "${GREEN}✓${NC} Grafana in docker-compose.yml"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} Grafana NOT in docker-compose.yml"
    ((FAILED++))
fi

echo ""
echo -e "${YELLOW}Checking Parent POM...${NC}"
if grep -q "<module>monitoring-service</module>" pom.xml; then
    echo -e "${GREEN}✓${NC} Monitoring service module in parent POM"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} Monitoring service module NOT in parent POM"
    ((FAILED++))
fi

echo ""
echo -e "${YELLOW}Checking Service Dependencies...${NC}"

# Check each service for Prometheus dependency
for service in github-service document-processor-service embedding-service milvus-service orchestrator-service; do
    if grep -q "micrometer-registry-prometheus" ${service}/pom.xml; then
        echo -e "${GREEN}✓${NC} ${service} has Prometheus dependency"
        ((PASSED++))
    else
        echo -e "${RED}✗${NC} ${service} MISSING Prometheus dependency"
        ((FAILED++))
    fi
done

echo ""
echo -e "${YELLOW}Checking Build Artifacts...${NC}"
if [ -f "monitoring-service/target/monitoring-service-1.0.0-SNAPSHOT.jar" ]; then
    echo -e "${GREEN}✓${NC} Monitoring service JAR built"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠${NC} Monitoring service JAR not built yet (run: mvn package)"
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Verification Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    echo -e "${GREEN}The monitoring system is properly configured.${NC}"
    echo ""
    echo -e "${BLUE}Next steps:${NC}"
    echo -e "  1. Build the project: ${YELLOW}mvn clean package -DskipTests${NC}"
    echo -e "  2. Start monitoring: ${YELLOW}./docs/scripts/start-monitoring.sh${NC}"
    echo -e "  3. Access Grafana: ${YELLOW}http://localhost:3000${NC} (admin/admin)"
    echo ""
    exit 0
else
    echo -e "${RED}✗ Some checks failed!${NC}"
    echo -e "${RED}Please review the errors above.${NC}"
    exit 1
fi

