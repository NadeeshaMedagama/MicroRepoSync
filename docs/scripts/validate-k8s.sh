#!/bin/bash

##############################################################################
# Kubernetes Manifest Validation Script
#
# This script validates Kubernetes manifests without requiring a cluster
# connection. It uses kubeconform for static validation.
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

# Check if kubeconform is installed
if ! command -v kubeconform &> /dev/null; then
    print_warning "kubeconform is not installed. Installing..."

    # Download and install kubeconform
    TEMP_DIR=$(mktemp -d)
    cd "$TEMP_DIR"

    wget -q https://github.com/yannh/kubeconform/releases/latest/download/kubeconform-linux-amd64.tar.gz
    tar xf kubeconform-linux-amd64.tar.gz

    if [ -w /usr/local/bin ]; then
        mv kubeconform /usr/local/bin/
    else
        sudo mv kubeconform /usr/local/bin/
    fi

    cd - > /dev/null
    rm -rf "$TEMP_DIR"

    print_success "kubeconform installed successfully"
fi

print_success "kubeconform found: $(kubeconform -v 2>&1)"

# Determine what to validate
TARGET="${1:-k8s}"

if [ ! -e "$TARGET" ]; then
    print_error "Path does not exist: $TARGET"
    exit 1
fi

echo ""
print_info "Starting Kubernetes manifest validation..."
echo ""

# Validate using kubeconform
if [ -f "$TARGET" ]; then
    # Single file validation
    print_info "Validating file: $TARGET"
    echo ""

    if kubeconform -summary -verbose "$TARGET"; then
        echo ""
        print_success "Manifest is valid! ✨"
        exit 0
    else
        echo ""
        print_error "Validation failed!"
        exit 1
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

    # Run kubeconform on all files
    if kubeconform -summary -verbose "${YAML_FILES[@]}"; then
        echo ""
        print_success "All manifests are valid! ✨"
        exit 0
    else
        echo ""
        print_error "Some manifests failed validation!"
        exit 1
    fi
fi

