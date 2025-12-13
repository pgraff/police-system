# Kafka Connect Plugins

This directory contains connector plugins for Kafka Connect. The connector JAR files placed here are automatically available to Kafka Connect via bind mount in docker-compose.

## OpenSearch Sink Connector

The OpenSearch Sink Connector is used to index events from Kafka topics into Elasticsearch/OpenSearch.

### Quick Installation

**The connector plugin is integrated into Docker Compose!** Just download the JAR file to this directory:

1. **Download the connector:**
   ```bash
   # Option 1: Use the download script (requires version number)
   ./scripts/download-opensearch-connector.sh <version>
   
   # Option 2: Manual download
   # Visit: https://github.com/opensearch-project/opensearch-kafka-connect/releases
   # Download the opensearch-sink-connector-*.jar file
   # Place it in: docker/kafka-connect/plugins/opensearch-sink/lib/
   ```

2. **Restart Kafka Connect** (no rebuild needed - uses bind mount):
   ```bash
   docker compose restart kafka-connect
   ```

3. **Verify installation:**
   ```bash
   curl http://localhost:8083/connector-plugins | jq '.[] | select(.class | contains("Opensearch"))'
   ```

You should see `org.opensearch.kafka.connect.OpensearchSinkConnector` in the list.

### Directory Structure

```
plugins/
  opensearch-sink/
    lib/
      opensearch-sink-connector-*.jar  ← Place JAR file here
```

### How It Works

- The `docker-compose.yml` includes a bind mount: `./docker/kafka-connect/plugins/opensearch-sink/lib:/kafka/connect/plugins/opensearch-sink/lib:ro`
- Any JAR files you place in `docker/kafka-connect/plugins/opensearch-sink/lib/` on your host are automatically available in the container
- No need to rebuild the image or copy files manually - just restart Kafka Connect after placing the JAR

### Finding the Latest Version

Visit the releases page to find the latest version:
https://github.com/opensearch-project/opensearch-kafka-connect/releases

Look for files named `opensearch-sink-connector-*.jar`

