#!/bin/bash

##############################################################################
# Kubernetes Manifest Validation Script
#
# This script validates Kubernetes manifests without requiring a cluster
# connection. It uses kubectl with --dry-run=client flag.
#
# Usage:
#   ./validate-k8s.sh                    # Validate all manifests in k8s/
#   ./validate-k8s.sh <path-to-file>     # Validate specific file
#   ./validate-k8s.sh <path-to-dir>      # Validate all YAML files in directory
##############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    print_error "kubectl is not installed. Please install kubectl first."
    echo "Visit: https://kubernetes.io/docs/tasks/tools/"
    exit 1
fi

print_success "kubectl found: $(kubectl version --client --short 2>/dev/null || kubectl version --client)"

# Determine what to validate
TARGET="${1:-k8s}"

if [ ! -e "$TARGET" ]; then
    print_error "Path does not exist: $TARGET"
    exit 1
fi

# Function to validate a single file
validate_file() {
    local file="$1"
    print_info "Validating: $file"

    if kubectl apply --dry-run=client -f "$file" > /dev/null 2>&1; then
        print_success "Valid: $file"
        return 0
    else
        print_error "Invalid: $file"
        echo "Error details:"
        kubectl apply --dry-run=client -f "$file" 2>&1 | sed 's/^/  /'
        return 1
    fi
}

# Main validation logic
FAILED_FILES=()
VALIDATED_COUNT=0

echo ""
print_info "Starting Kubernetes manifest validation..."
echo ""

if [ -f "$TARGET" ]; then
    # Single file validation
    if validate_file "$TARGET"; then
        VALIDATED_COUNT=1
    else
        FAILED_FILES+=("$TARGET")
    fi
elif [ -d "$TARGET" ]; then
    # Directory validation
    shopt -s nullglob
    YAML_FILES=("$TARGET"/*.yaml "$TARGET"/*.yml)

    if [ ${#YAML_FILES[@]} -eq 0 ]; then
        print_warning "No YAML files found in: $TARGET"
        exit 0
    fi

    print_info "Found ${#YAML_FILES[@]} YAML file(s) to validate"
    echo ""

    for file in "${YAML_FILES[@]}"; do
        if validate_file "$file"; then
            ((VALIDATED_COUNT++))
        else
            FAILED_FILES+=("$file")
        fi
        echo ""
    done
fi

# Print summary
echo ""
echo "================================================"
echo "             Validation Summary"
echo "================================================"
echo ""
print_info "Total files validated: $VALIDATED_COUNT"

if [ ${#FAILED_FILES[@]} -eq 0 ]; then
    print_success "All manifests are valid! ✨"
    echo ""
    exit 0
else
    print_error "${#FAILED_FILES[@]} file(s) failed validation:"
    for file in "${FAILED_FILES[@]}"; do
        echo "  - $file"
    done
    echo ""
    print_info "Tip: Use 'kubectl apply --dry-run=client -f <file>' for detailed error messages"
    echo ""
    exit 1
fi

