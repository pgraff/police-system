# Kafka Connect Plugins

This directory contains connector plugins for Kafka Connect.

## OpenSearch Sink Connector

The OpenSearch Sink Connector is used to index events from Kafka topics into Elasticsearch/OpenSearch.

### Installation

1. Download the latest release from: https://github.com/opensearch-project/opensearch-kafka-connect/releases
2. Extract the JAR files to `docker/kafka-connect/plugins/opensearch-sink/lib/`
3. Ensure all dependencies are included

### Directory Structure

```
plugins/
  opensearch-sink/
    lib/
      opensearch-sink-connector-*.jar
      (dependencies)
```

### Alternative: Using Docker Volume

If you prefer to download the connector directly into the volume:

```bash
# Start Kafka Connect (it will create the volume)
docker-compose up -d kafka-connect

# Copy connector JARs into the volume
docker cp opensearch-sink-connector-*.jar kafka-connect:/kafka/connect/plugins/opensearch-sink/lib/

# Restart Kafka Connect to load the plugin
docker-compose restart kafka-connect
```

### Verification

After installing the connector, verify it's loaded:

```bash
curl http://localhost:8083/connector-plugins | jq '.[] | select(.class | contains("Opensearch"))'
```

You should see `org.opensearch.kafka.connect.OpensearchSinkConnector` in the list.

