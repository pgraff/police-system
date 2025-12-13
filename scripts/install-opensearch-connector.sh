#!/bin/bash

# Script to download and install OpenSearch Sink Connector for Kafka Connect
# This connector works with both OpenSearch and Elasticsearch 7.x+

set -e

PLUGIN_DIR="${PLUGIN_DIR:-docker/kafka-connect/plugins/opensearch-sink/lib}"
RELEASE_URL="https://api.github.com/repos/opensearch-project/opensearch-kafka-connect/releases/latest"

echo "=== Installing OpenSearch Sink Connector ==="
echo "Plugin directory: $PLUGIN_DIR"
echo ""

# Create plugin directory if it doesn't exist
mkdir -p "$PLUGIN_DIR"

# Check if connector is already installed
if [ -f "$PLUGIN_DIR"/opensearch-sink-connector-*.jar ]; then
    echo "OpenSearch connector appears to be already installed."
    echo "Existing JAR files:"
    ls -lh "$PLUGIN_DIR"/opensearch-sink-connector-*.jar 2>/dev/null || true
    read -p "Do you want to reinstall? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Installation cancelled."
        exit 0
    fi
    echo "Removing existing connector files..."
    rm -f "$PLUGIN_DIR"/opensearch-sink-connector-*.jar
fi

echo "Fetching latest release information..."
LATEST_RELEASE=$(curl -s "$RELEASE_URL")

# Extract download URL for the JAR file
DOWNLOAD_URL=$(echo "$LATEST_RELEASE" | grep -o '"browser_download_url": "[^"]*opensearch-sink-connector[^"]*\.jar"' | head -1 | cut -d'"' -f4)

if [ -z "$DOWNLOAD_URL" ]; then
    echo "ERROR: Could not find download URL for the connector JAR."
    echo "Please download manually from: https://github.com/opensearch-project/opensearch-kafka-connect/releases"
    echo ""
    echo "Then extract the JAR file to: $PLUGIN_DIR"
    exit 1
fi

echo "Found download URL: $DOWNLOAD_URL"
echo "Downloading connector..."

# Download the connector JAR
TEMP_FILE=$(mktemp)
curl -L -o "$TEMP_FILE" "$DOWNLOAD_URL"

# Check if download was successful
if [ ! -f "$TEMP_FILE" ] || [ ! -s "$TEMP_FILE" ]; then
    echo "ERROR: Download failed."
    rm -f "$TEMP_FILE"
    exit 1
fi

# Move to plugin directory
FILENAME=$(basename "$DOWNLOAD_URL")
mv "$TEMP_FILE" "$PLUGIN_DIR/$FILENAME"

echo "✓ Connector downloaded successfully: $FILENAME"
echo ""

# Check for dependencies
echo "Checking for dependencies..."
# The connector may require additional dependencies
# Common dependencies are usually included in the JAR, but check the release notes

echo ""
echo "=== Installation Complete ==="
echo "Connector installed to: $PLUGIN_DIR"
echo ""
echo "Next steps:"
echo "1. If Kafka Connect is running, restart it:"
echo "   docker compose restart kafka-connect"
echo ""
echo "2. Verify the connector is loaded:"
echo "   curl http://localhost:8083/connector-plugins | jq '.[] | select(.class | contains(\"Opensearch\"))'"
echo ""
echo "3. Deploy connectors:"
echo "   ./scripts/deploy-connectors.sh"

