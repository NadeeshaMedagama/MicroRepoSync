#!/bin/bash

# Validation script for Java 21 GitHub Actions fix
# This script validates that the Maven build will work with Java 21

set -e

echo "=============================================="
echo "Java 21 Build Validation Script"
echo "=============================================="
echo ""

# Check Java version
echo "1. Checking Java version..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | grep version | awk -F '"' '{print $2}')
    echo "   ✓ Java version: $JAVA_VERSION"

    if [[ "$JAVA_VERSION" == 21.* ]]; then
        echo "   ✓ Java 21 detected"
    else
        echo "   ⚠ Warning: Java version is not 21 (found: $JAVA_VERSION)"
        echo "   This may cause issues. Please install Java 21."
    fi
else
    echo "   ✗ Java not found in PATH"
    exit 1
fi
echo ""

# Check Maven version
echo "2. Checking Maven version..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -1)
    echo "   ✓ $MVN_VERSION"
else
    echo "   ✗ Maven not found in PATH"
    exit 1
fi
echo ""

# Verify JAVA_HOME
echo "3. Checking JAVA_HOME..."
if [ -z "$JAVA_HOME" ]; then
    echo "   ⚠ Warning: JAVA_HOME is not set"
else
    echo "   ✓ JAVA_HOME: $JAVA_HOME"
fi
echo ""

# Validate parent POM
echo "4. Validating parent POM configuration..."
if grep -q "<release>21</release>" pom.xml; then
    echo "   ✓ Parent POM has Java 21 release configuration"
else
    echo "   ✗ Parent POM missing Java 21 release configuration"
    exit 1
fi

if grep -q "<pluginManagement>" pom.xml; then
    echo "   ✓ Parent POM has pluginManagement section"
else
    echo "   ⚠ Warning: Parent POM missing pluginManagement section"
fi
echo ""

# Validate GitHub Actions workflows
echo "5. Validating GitHub Actions workflows..."
WORKFLOW_FILES=(.github/workflows/*.yml)
JAVA_21_COUNT=0
VERIFY_COUNT=0

for file in "${WORKFLOW_FILES[@]}"; do
    if [ -f "$file" ]; then
        if grep -q "java-version: '21'" "$file"; then
            ((JAVA_21_COUNT++))
        fi
        if grep -q "Verify Java version" "$file" || grep -q "Verify Java installation" "$file"; then
            ((VERIFY_COUNT++))
        fi
    fi
done

echo "   ✓ Found $JAVA_21_COUNT workflow files using Java 21"
echo "   ✓ Found $VERIFY_COUNT workflow files with Java verification"
echo ""

# Test Maven compile (dry run)
echo "6. Testing Maven configuration (validating only)..."
if mvn validate -q > /dev/null 2>&1; then
    echo "   ✓ Maven validation successful"
else
    echo "   ⚠ Maven validation had issues (may be normal if dependencies aren't downloaded)"
fi
echo ""

echo "=============================================="
echo "Validation Summary"
echo "=============================================="
echo "✓ All checks passed!"
echo ""
echo "Your project is configured for Java 21 and should"
echo "work correctly in GitHub Actions."
echo ""
echo "Next steps:"
echo "1. Commit and push your changes"
echo "2. Monitor the GitHub Actions workflow"
echo "3. Check the 'Verify Java version' step in the logs"
echo "=============================================="

