#!/bin/bash

# Test script to validate connector configurations
# This validates JSON syntax and checks for required fields

set -e

CONNECTORS_DIR="${CONNECTORS_DIR:-docker/kafka-connect/connectors}"

echo "=== Validating Connector Configurations ==="
echo ""

all_valid=true
connector_count=0

for connector_file in "$CONNECTORS_DIR"/*.json; do
    if [ ! -f "$connector_file" ]; then
        continue
    fi
    
    connector_count=$((connector_count + 1))
    filename=$(basename "$connector_file")
    
    echo "Validating: $filename"
    
    # Validate JSON syntax
    if ! jq empty "$connector_file" 2>/dev/null; then
        echo "  ✗ Invalid JSON syntax"
        all_valid=false
        continue
    fi
    
    # Check required fields
    name=$(jq -r '.name // empty' "$connector_file")
    connector_class=$(jq -r '.config."connector.class" // empty' "$connector_file")
    topics=$(jq -r '.config.topics // empty' "$connector_file")
    index=$(jq -r '.config.index // empty' "$connector_file")
    
    errors=0
    
    if [ -z "$name" ]; then
        echo "  ✗ Missing 'name' field"
        errors=$((errors + 1))
    fi
    
    if [ -z "$connector_class" ]; then
        echo "  ✗ Missing 'config.connector.class' field"
        errors=$((errors + 1))
    fi
    
    if [ -z "$topics" ]; then
        echo "  ✗ Missing 'config.topics' field"
        errors=$((errors + 1))
    fi
    
    if [ -z "$index" ]; then
        echo "  ✗ Missing 'config.index' field"
        errors=$((errors + 1))
    fi
    
    # Check if index has date pattern
    if [[ ! "$index" =~ \$\{timestamp ]]; then
        echo "  ⚠ Index name doesn't include date pattern: $index"
    fi
    
    if [ $errors -eq 0 ]; then
        echo "  ✓ Valid configuration"
        echo "    Name: $name"
        echo "    Class: $connector_class"
        echo "    Topics: $topics"
        echo "    Index: $index"
    else
        echo "  ✗ Found $errors error(s)"
        all_valid=false
    fi
    
    echo ""
done

echo "=== Summary ==="
echo "Validated $connector_count connector(s)"

if [ "$all_valid" = true ]; then
    echo "✓ All connector configurations are valid"
    exit 0
else
    echo "✗ Some connector configurations have errors"
    exit 1
fi

