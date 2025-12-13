# Quick Start: Installing OpenSearch Connector

The connector plugin needs to be downloaded manually. Here's the fastest way:

## Step 1: Download the Connector

**Option A: Using wget/curl (if you know the version):**
```bash
mkdir -p docker/kafka-connect/plugins/opensearch-sink/lib
cd docker/kafka-connect/plugins/opensearch-sink/lib

# Try one of these URLs (check GitHub for the latest):
wget https://github.com/opensearch-project/opensearch-kafka-connect/releases/download/v2.3.0.0/opensearch-sink-connector-2.3.0.0.jar
```

**Option B: Manual Download:**
1. Visit: https://github.com/opensearch-project/opensearch-kafka-connect/releases
2. Find the latest release
3. Download the `opensearch-sink-connector-*.jar` file
4. Place it in: `docker/kafka-connect/plugins/opensearch-sink/lib/`

## Step 2: Restart Kafka Connect

```bash
docker compose restart kafka-connect
```

## Step 3: Verify Plugin is Loaded

```bash
curl http://localhost:8083/connector-plugins | jq '.[] | select(.class | contains("Opensearch"))'
```

You should see the connector class listed.

## Step 4: Deploy Connectors

```bash
./scripts/deploy-connectors.sh
```

## Step 5: Check Health

```bash
./scripts/check-connectors.sh
```

## Alternative: Use Confluent Elasticsearch Connector

If you prefer to use Confluent's connector (compatible with Elasticsearch):

1. The connector configurations use `org.opensearch.kafka.connect.OpensearchSinkConnector`
2. You can modify them to use `io.confluent.connect.elasticsearch.ElasticsearchSinkConnector` instead
3. Install via: `confluent-hub install confluentinc/kafka-connect-elasticsearch:latest`

However, the OpenSearch connector is recommended as it's designed for both OpenSearch and Elasticsearch.

