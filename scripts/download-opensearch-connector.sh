#!/bin/bash

# Simple script to download OpenSearch Sink Connector
# Downloads to the directory that's bind-mounted in docker-compose

set -e

PLUGIN_DIR="docker/kafka-connect/plugins/opensearch-sink/lib"
VERSION="${1}"

echo "=== Downloading OpenSearch Sink Connector ==="
echo ""

# Create directory
mkdir -p "$PLUGIN_DIR"

# If version not provided, try to detect latest
if [ -z "$VERSION" ]; then
    echo "No version specified. Attempting to find latest release..."
    echo "Visit https://github.com/opensearch-project/opensearch-kafka-connect/releases to find the latest version"
    echo ""
    echo "Usage: $0 <version>"
    echo "Example: $0 2.3.0.0"
    echo ""
    echo "Or download manually:"
    echo "1. Go to: https://github.com/opensearch-project/opensearch-kafka-connect/releases"
    echo "2. Download the opensearch-sink-connector-*.jar file"
    echo "3. Place it in: $PLUGIN_DIR"
    exit 1
fi

# Try different URL patterns
DOWNLOAD_URLS=(
    "https://github.com/opensearch-project/opensearch-kafka-connect/releases/download/v${VERSION}/opensearch-sink-connector-${VERSION}.jar"
    "https://github.com/opensearch-project/opensearch-kafka-connect/releases/download/${VERSION}/opensearch-sink-connector-${VERSION}.jar"
    "https://github.com/opensearch-project/opensearch-kafka-connect/releases/download/opensearch-kafka-connect-${VERSION}/opensearch-sink-connector-${VERSION}.jar"
)

echo "Version: $VERSION"
echo "Target directory: $PLUGIN_DIR"
echo ""

DOWNLOADED=false
for DOWNLOAD_URL in "${DOWNLOAD_URLS[@]}"; do
    echo "Trying: $DOWNLOAD_URL"
    if curl -L -f -o "$PLUGIN_DIR/opensearch-sink-connector-${VERSION}.jar" "$DOWNLOAD_URL" 2>/dev/null; then
        # Verify it's actually a JAR (not a 404 page)
        SIZE=$(stat -c%s "$PLUGIN_DIR/opensearch-sink-connector-${VERSION}.jar" 2>/dev/null || echo "0")
        if [ "$SIZE" -gt 1000000 ]; then
            echo ""
            echo "✓ Download successful!"
            DOWNLOADED=true
            break
        else
            rm -f "$PLUGIN_DIR/opensearch-sink-connector-${VERSION}.jar"
            echo "  Failed (file too small - likely 404 page)"
        fi
    else
        echo "  Failed"
    fi
done

if [ "$DOWNLOADED" = true ]; then
    echo "File: $PLUGIN_DIR/opensearch-sink-connector-${VERSION}.jar"
    ls -lh "$PLUGIN_DIR/opensearch-sink-connector-${VERSION}.jar"
    echo ""
    echo "Next steps:"
    echo "1. Restart Kafka Connect: docker compose restart kafka-connect"
    echo "2. Verify plugin: curl http://localhost:8083/connector-plugins | jq '.[] | select(.class | contains(\"Opensearch\"))'"
    echo "3. Deploy connectors: ./scripts/deploy-connectors.sh"
else
    echo ""
    echo "✗ Download failed for all URL patterns!"
    echo ""
    echo "Please download manually:"
    echo "1. Visit: https://github.com/opensearch-project/opensearch-kafka-connect/releases"
    echo "2. Find the latest release and download the opensearch-sink-connector-*.jar file"
    echo "3. Place it in: $PLUGIN_DIR"
    echo "4. Restart: docker compose restart kafka-connect"
    exit 1
fi

