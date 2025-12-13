#!/bin/bash

# Complete setup script for OpenSearch connector
# This script guides you through downloading and setting up the connector

set -e

PLUGIN_DIR="docker/kafka-connect/plugins/opensearch-sink/lib"

echo "=== OpenSearch Connector Setup ==="
echo ""

# Check if connector already exists
if ls "$PLUGIN_DIR"/*.jar 1> /dev/null 2>&1; then
    echo "✓ Connector JAR found:"
    ls -lh "$PLUGIN_DIR"/*.jar
    echo ""
    read -p "Do you want to reinstall? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Keeping existing connector."
        exit 0
    fi
    rm -f "$PLUGIN_DIR"/*.jar
fi

echo "To download the connector, you have two options:"
echo ""
echo "Option 1: Manual Download (Recommended)"
echo "  1. Visit: https://github.com/opensearch-project/opensearch-kafka-connect/releases"
echo "  2. Find the latest release"
echo "  3. Download the file: opensearch-sink-connector-*.jar"
echo "  4. Place it in: $PLUGIN_DIR"
echo ""
echo "Option 2: Try Automated Download"
echo "  The script will attempt to download from a known version..."
echo ""

# Try a few known version numbers
KNOWN_VERSIONS=("2.3.0.0" "2.2.0.0" "2.1.0.0" "2.0.0.0")

for VERSION in "${KNOWN_VERSIONS[@]}"; do
    echo "Trying version $VERSION..."
    URL="https://github.com/opensearch-project/opensearch-kafka-connect/releases/download/v${VERSION}/opensearch-sink-connector-${VERSION}.jar"
    
    if curl -L -f -o "$PLUGIN_DIR/opensearch-sink-connector-${VERSION}.jar" "$URL" 2>/dev/null; then
        SIZE=$(stat -c%s "$PLUGIN_DIR/opensearch-sink-connector-${VERSION}.jar" 2>/dev/null || echo "0")
        if [ "$SIZE" -gt 1000000 ]; then
            echo "✓ Successfully downloaded version $VERSION!"
            echo "File: $PLUGIN_DIR/opensearch-sink-connector-${VERSION}.jar"
            ls -lh "$PLUGIN_DIR/opensearch-sink-connector-${VERSION}.jar"
            echo ""
            echo "Next: Restart Kafka Connect and deploy connectors"
            exit 0
        else
            rm -f "$PLUGIN_DIR/opensearch-sink-connector-${VERSION}.jar"
            echo "  Failed (file too small)"
        fi
    else
        echo "  Failed"
    fi
done

echo ""
echo "✗ Automated download failed for all known versions."
echo ""
echo "Please download manually:"
echo "  1. Visit: https://github.com/opensearch-project/opensearch-kafka-connect/releases"
echo "  2. Download the latest opensearch-sink-connector-*.jar"
echo "  3. Place it in: $PLUGIN_DIR"
echo "  4. Then run: docker compose restart kafka-connect"
exit 1

