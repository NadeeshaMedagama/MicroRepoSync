#!/bin/bash

#######################################################################
# Dependency Updates Pipeline - Integration Test Script
#######################################################################
# This script verifies that the dependency updates pipeline has been
# successfully integrated without conflicts with existing pipelines.
#######################################################################

echo "=========================================="
echo "Dependency Updates Pipeline Integration Test"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Function to print test result
test_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ PASS${NC}: $2"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        echo -e "${RED}❌ FAIL${NC}: $2"
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
}

echo "1. Checking workflow files..."
echo "------------------------------"

# Test 1: Check if all workflows exist
if [ -f ".github/workflows/ci-cd.yml" ] && \
   [ -f ".github/workflows/daily-sync.yml" ] && \
   [ -f ".github/workflows/dependency-updates.yml" ]; then
    test_result 0 "All workflow files exist"
else
    test_result 1 "Missing workflow files"
fi

# Test 2: Check Renovate config
if [ -f ".github/renovate.json" ]; then
    test_result 0 "Renovate configuration exists"
else
    test_result 1 "Renovate configuration missing"
fi

# Test 3: Check suppression file
if [ -f "dependency-check-suppressions.xml" ]; then
    test_result 0 "OWASP suppression file exists"
else
    test_result 1 "OWASP suppression file missing"
fi

echo ""
echo "2. Validating POM configuration..."
echo "-----------------------------------"

# Test 4: Validate POM file
if mvn validate -q 2>&1 | grep -q "BUILD SUCCESS"; then
    test_result 0 "POM validation successful"
else
    test_result 1 "POM validation failed"
fi

# Test 5: Check for OWASP plugin
if grep -q "dependency-check-maven" pom.xml; then
    test_result 0 "OWASP Dependency Check plugin configured"
else
    test_result 1 "OWASP Dependency Check plugin missing"
fi

# Test 6: Check for Versions plugin
if grep -q "versions-maven-plugin" pom.xml; then
    test_result 0 "Versions Maven plugin configured"
else
    test_result 1 "Versions Maven plugin missing"
fi

# Test 7: Check for License plugin
if grep -q "license-maven-plugin" pom.xml; then
    test_result 0 "License Maven plugin configured"
else
    test_result 1 "License Maven plugin missing"
fi

echo ""
echo "3. Testing Maven plugins..."
echo "----------------------------"

# Test 8: Test versions plugin
echo "   Testing versions:display-dependency-updates..."
if timeout 60 mvn versions:display-dependency-updates -q > /dev/null 2>&1; then
    test_result 0 "Versions plugin working"
else
    test_result 1 "Versions plugin not working"
fi

# Test 9: Test dependency tree
echo "   Testing dependency:tree..."
if timeout 30 mvn dependency:tree -q > /dev/null 2>&1; then
    test_result 0 "Dependency tree generation working"
else
    test_result 1 "Dependency tree generation failed"
fi

echo ""
echo "4. Checking documentation..."
echo "-----------------------------"

# Test 10: Check documentation files
DOCS_EXIST=0
[ -f "docs/readmes/DEPENDENCY_UPDATES_PIPELINE.md" ] && DOCS_EXIST=$((DOCS_EXIST + 1))
[ -f "docs/readmes/DEPENDENCY_UPDATES_QUICKSTART.md" ] && DOCS_EXIST=$((DOCS_EXIST + 1))
[ -f "docs/readmes/IMPLEMENTATION_SUMMARY.md" ] && DOCS_EXIST=$((DOCS_EXIST + 1))
[ -f "docs/readmes/INTEGRATION_VERIFICATION.md" ] && DOCS_EXIST=$((DOCS_EXIST + 1))

if [ $DOCS_EXIST -eq 4 ]; then
    test_result 0 "All documentation files present"
else
    test_result 1 "Missing documentation files ($DOCS_EXIST/4 found)"
fi

# Test 11: Check README updates
if grep -q "Dependency Updates" README.md; then
    test_result 0 "README updated with dependency info"
else
    test_result 1 "README not updated"
fi

echo ""
echo "5. Workflow configuration analysis..."
echo "--------------------------------------"

# Test 12: Check workflow schedules (no conflicts)
CI_CD_TRIGGER=$(grep -A 2 "^on:" .github/workflows/ci-cd.yml | grep "push\|pull_request" | wc -l)
DAILY_SYNC_CRON=$(grep "cron:" .github/workflows/daily-sync.yml | grep "0 8" | wc -l)
DEP_UPDATE_CRON=$(grep "cron:" .github/workflows/dependency-updates.yml | grep "0 9" | wc -l)

if [ $CI_CD_TRIGGER -gt 0 ] && [ $DAILY_SYNC_CRON -eq 1 ] && [ $DEP_UPDATE_CRON -eq 1 ]; then
    test_result 0 "No schedule conflicts detected"
else
    test_result 1 "Schedule conflict detected"
fi

# Test 13: Check Java versions
if grep -q "java-version: '21'" .github/workflows/dependency-updates.yml; then
    test_result 0 "Dependency updates uses Java 21"
else
    test_result 1 "Wrong Java version in dependency updates"
fi

echo ""
echo "6. Workflow job verification..."
echo "--------------------------------"

# Test 14: Check all jobs exist
JOBS=("dependency-check" "maven-dependency-tree" "security-audit" "license-check" "create-update-pr" "renovate-config" "summary")
JOBS_FOUND=0

for job in "${JOBS[@]}"; do
    if grep -q "$job:" .github/workflows/dependency-updates.yml; then
        JOBS_FOUND=$((JOBS_FOUND + 1))
    fi
done

if [ $JOBS_FOUND -eq 7 ]; then
    test_result 0 "All 7 workflow jobs configured"
else
    test_result 1 "Missing workflow jobs ($JOBS_FOUND/7 found)"
fi

echo ""
echo "=========================================="
echo "           TEST SUMMARY"
echo "=========================================="
echo ""
echo -e "${GREEN}Passed: $TESTS_PASSED${NC}"
echo -e "${RED}Failed: $TESTS_FAILED${NC}"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}=========================================="
    echo "✅ ALL TESTS PASSED!"
    echo "=========================================="
    echo ""
    echo "The Dependency Updates Pipeline has been"
    echo "successfully integrated with no conflicts."
    echo ""
    echo "Next steps:"
    echo "  1. Go to GitHub Actions tab"
    echo "  2. Select 'Dependency Updates & Security Checks'"
    echo "  3. Click 'Run workflow' to test manually"
    echo "  4. Wait for next Monday 9 AM UTC for automatic run"
    echo -e "==========================================${NC}"
    exit 0
else
    echo -e "${RED}=========================================="
    echo "❌ SOME TESTS FAILED"
    echo "=========================================="
    echo ""
    echo "Please review the failed tests above and"
    echo "ensure all components are properly configured."
    echo -e "==========================================${NC}"
    exit 1
fi

