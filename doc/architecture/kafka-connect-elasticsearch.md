# Kafka Connect Elasticsearch Indexer

## Overview

Kafka Connect is used to automatically index all events from Kafka topics into Elasticsearch for searchable event history and analytics. The system uses the OpenSearch Sink Connector (compatible with Elasticsearch 7.x+) to consume events from all 14 event topics and index them into domain-specific Elasticsearch indices with daily rollover.

## Architecture

```
Kafka Topics (14 topics)
    ↓
Kafka Connect (Distributed Mode)
    ↓
OpenSearch Sink Connector
    ↓
Elasticsearch (Domain-specific indices with daily rollover)
    ↓
Dead Letter Topics (Error handling)
```

## Components

### Kafka Connect Service

**Service**: `kafka-connect`  
**Port**: 8083  
**REST API**: http://localhost:8083

**Configuration**:
- Distributed mode (high availability)
- Connects to all 3 Kafka brokers
- Plugin path includes OpenSearch connector
- Uses StringConverter for JSON events

### Connectors

14 connectors, one per event topic:

1. `officer-events-to-elasticsearch` → `officer-events` index
2. `vehicle-events-to-elasticsearch` → `vehicle-events` index
3. `unit-events-to-elasticsearch` → `unit-events` index
4. `person-events-to-elasticsearch` → `person-events` index
5. `location-events-to-elasticsearch` → `location-events` index
6. `incident-events-to-elasticsearch` → `incident-events` index
7. `call-events-to-elasticsearch` → `call-events` index
8. `activity-events-to-elasticsearch` → `activity-events` index
9. `assignment-events-to-elasticsearch` → `assignment-events` index
10. `shift-events-to-elasticsearch` → `shift-events` index
11. `officer-shift-events-to-elasticsearch` → `officer-shift-events` index
12. `dispatch-events-to-elasticsearch` → `dispatch-events` index
13. `resource-assignment-events-to-elasticsearch` → `resource-assignment-events` index
14. `involved-party-events-to-elasticsearch` → `involved-party-events` index

### Elasticsearch Indices

**Index Naming Pattern**: `{domain}-events-{YYYY.MM.DD}`

Examples:
- `officer-events-2024.12.12`
- `incident-events-2024.12.12`
- `call-events-2024.12.12`

**Index Lifecycle Management (ILM)**:
- **Hot Phase**: 7 days (frequent queries, high priority)
- **Warm Phase**: 30 days (less frequent queries, reduced replicas)
- **Delete Phase**: 90 days (automatic deletion)

### Dead Letter Topics

One dead letter topic per domain for error handling:
- `dlq-officer-events`
- `dlq-vehicle-events`
- ... (14 total)

Failed events are sent to DLQ topics for debugging and reprocessing.

## Setup

### Prerequisites

1. **Elasticsearch** must be running and accessible
2. **Kafka Connect** service must be started
3. **OpenSearch Sink Connector** plugin must be installed

### Installing the Connector Plugin

1. Download the latest OpenSearch Sink Connector from:
   https://github.com/opensearch-project/opensearch-kafka-connect/releases

2. Extract JAR files to the plugin directory:
   ```bash
   # Option 1: Copy to local directory (mounted as volume)
   mkdir -p docker/kafka-connect/plugins/opensearch-sink/lib
   cp opensearch-sink-connector-*.jar docker/kafka-connect/plugins/opensearch-sink/lib/
   
   # Option 2: Copy directly to running container
   docker cp opensearch-sink-connector-*.jar kafka-connect:/kafka/connect/plugins/opensearch-sink/lib/
   ```

3. Restart Kafka Connect:
   ```bash
   docker compose restart kafka-connect
   ```

4. Verify plugin is loaded:
   ```bash
   curl http://localhost:8083/connector-plugins | jq '.[] | select(.class | contains("Opensearch"))'
   ```

### Deploying Connectors

Use the deployment script to set up all connectors:

```bash
# Set environment variables (if needed)
export ELASTICSEARCH_URL=http://elasticsearch:9200
export ELASTICSEARCH_USERNAME=elastic
export ELASTICSEARCH_PASSWORD=changeme

# Run deployment script
./scripts/deploy-connectors.sh
```

The script will:
1. Wait for Kafka Connect to be ready
2. Create dead letter topics
3. Apply Elasticsearch index templates and ILM policies
4. Register all 14 connectors
5. Verify connector status

### Manual Connector Registration

To register a single connector manually:

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @docker/kafka-connect/connectors/officer-events-connector.json
```

## Monitoring

### Check Connector Status

```bash
# List all connectors
curl http://localhost:8083/connectors | jq

# Check specific connector status
curl http://localhost:8083/connectors/officer-events-to-elasticsearch/status | jq

# Check connector configuration
curl http://localhost:8083/connectors/officer-events-to-elasticsearch/config | jq
```

### Health Check Script

Run the health check script to verify all connectors:

```bash
./scripts/check-connectors.sh
```

This checks:
- All connectors are RUNNING
- No failed tasks
- Dead letter topics are empty
- Elasticsearch indices have documents

### View Connector Logs

```bash
docker logs kafka-connect -f
```

### Check Dead Letter Topics

```bash
# List messages in a dead letter topic
docker exec -it kafka-broker-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic dlq-officer-events \
  --from-beginning

# Count messages in DLQ
docker exec kafka-broker-1 kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic dlq-officer-events \
  --time -1 | awk -F: '{sum += $3} END {print sum}'
```

### Check Elasticsearch Indices

```bash
# List all indices
curl http://localhost:9200/_cat/indices?v

# Count documents in an index
curl http://localhost:9200/officer-events-*/_count | jq

# Search for events
curl -X POST http://localhost:9200/officer-events-*/_search \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "match": {
        "eventType": "RegisterOfficerRequested"
      }
    }
  }' | jq
```

## Configuration

### Connector Configuration

Each connector configuration includes:

- **Connector Class**: `org.opensearch.kafka.connect.OpensearchSinkConnector`
- **Tasks**: 2 (parallel processing)
- **Topic**: Domain-specific event topic
- **Index**: Domain-specific index name
- **Document ID**: `eventId` (for idempotency)
- **Error Handling**: Dead letter topics enabled
- **Converters**: StringConverter (events are JSON strings)

### Environment Variables

The deployment script supports these environment variables:

- `KAFKA_CONNECT_URL`: Kafka Connect REST API URL (default: http://localhost:8083)
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers (default: localhost:9092)
- `ELASTICSEARCH_URL`: Elasticsearch URL (default: http://localhost:9200)
- `ELASTICSEARCH_USERNAME`: Elasticsearch username (optional)
- `ELASTICSEARCH_PASSWORD`: Elasticsearch password (optional)

### Index Templates

Index templates are defined in `docker/kafka-connect/elasticsearch/templates/`:

- Field mappings for `eventId`, `eventType`, `timestamp`, `aggregateId`, `version`
- ILM policy reference
- Index settings (shards, replicas, refresh interval)

### ILM Policy

The ILM policy (`police-events-policy`) is defined in `docker/kafka-connect/elasticsearch/policies/`:

- **Hot Phase**: 7 days, high priority, frequent queries
- **Warm Phase**: 30 days, reduced replicas
- **Delete Phase**: 90 days, automatic deletion

## Troubleshooting

### Connector Not Starting

1. **Check plugin is installed**:
   ```bash
   curl http://localhost:8083/connector-plugins | jq '.[] | select(.class | contains("Opensearch"))'
   ```

2. **Check connector status**:
   ```bash
   curl http://localhost:8083/connectors/{connector-name}/status | jq
   ```

3. **Check logs**:
   ```bash
   docker logs kafka-connect | grep -i error
   ```

### Events Not Indexing

1. **Check connector is running**:
   ```bash
   curl http://localhost:8083/connectors/{connector-name}/status | jq '.connector.state'
   ```

2. **Check Elasticsearch connection**:
   ```bash
   curl http://localhost:9200
   ```

3. **Check dead letter topics**:
   ```bash
   ./scripts/check-connectors.sh
   ```

4. **Verify events are in Kafka**:
   ```bash
   docker exec -it kafka-broker-1 kafka-console-consumer \
     --bootstrap-server localhost:9092 \
     --topic officer-events \
     --from-beginning \
     --max-messages 5
   ```

### Dead Letter Topic Has Messages

1. **View error messages**:
   ```bash
   docker exec -it kafka-broker-1 kafka-console-consumer \
     --bootstrap-server localhost:9092 \
     --topic dlq-officer-events \
     --from-beginning
   ```

2. **Check connector logs for errors**:
   ```bash
   docker logs kafka-connect | grep -i error
   ```

3. **Common issues**:
   - Malformed JSON in events
   - Elasticsearch connection issues
   - Index template not applied
   - Missing `eventId` field in events

### Index Template Not Applied

1. **Check if template exists**:
   ```bash
   curl http://localhost:9200/_index_template/officer-events-template | jq
   ```

2. **Apply template manually**:
   ```bash
   curl -X PUT http://localhost:9200/_index_template/officer-events-template \
     -H "Content-Type: application/json" \
     -d @docker/kafka-connect/elasticsearch/templates/officer-events-template.json
   ```

### ILM Policy Not Working

1. **Check if policy exists**:
   ```bash
   curl http://localhost:9200/_ilm/policy/police-events-policy | jq
   ```

2. **Apply policy manually**:
   ```bash
   curl -X PUT http://localhost:9200/_ilm/policy/police-events-policy \
     -H "Content-Type: application/json" \
     -d @docker/kafka-connect/elasticsearch/policies/police-events-policy.json
   ```

## Updating Connectors

### Update Connector Configuration

```bash
# Update a connector's configuration
curl -X PUT http://localhost:8083/connectors/{connector-name}/config \
  -H "Content-Type: application/json" \
  -d @docker/kafka-connect/connectors/{connector-name}-connector.json
```

### Restart a Connector

```bash
# Restart connector (stops and starts)
curl -X POST http://localhost:8083/connectors/{connector-name}/restart

# Restart connector tasks only
curl -X POST http://localhost:8083/connectors/{connector-name}/tasks/0/restart
```

### Delete a Connector

```bash
# Delete connector (stops and removes)
curl -X DELETE http://localhost:8083/connectors/{connector-name}
```

## Querying Elasticsearch

### Search by Event Type

```bash
curl -X POST http://localhost:9200/officer-events-*/_search \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "term": {
        "eventType": "RegisterOfficerRequested"
      }
    }
  }' | jq
```

### Search by Aggregate ID

```bash
curl -X POST http://localhost:9200/incident-events-*/_search \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "term": {
        "aggregateId": "INCIDENT-12345"
      }
    },
    "sort": [
      {
        "timestamp": {
          "order": "asc"
        }
      }
    ]
  }' | jq
```

### Search by Date Range

```bash
curl -X POST http://localhost:9200/officer-events-*/_search \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "range": {
        "timestamp": {
          "gte": "2024-12-01T00:00:00Z",
          "lte": "2024-12-31T23:59:59Z"
        }
      }
    }
  }' | jq
```

### Aggregate Queries

```bash
# Count events by type
curl -X POST http://localhost:9200/officer-events-*/_search \
  -H "Content-Type: application/json" \
  -d '{
    "size": 0,
    "aggs": {
      "event_types": {
        "terms": {
          "field": "eventType",
          "size": 10
        }
      }
    }
  }' | jq
```

## Production Considerations

1. **Security**: Configure Elasticsearch authentication and TLS
2. **Performance**: Tune connector tasks and batch sizes
3. **Monitoring**: Set up alerts for connector failures and DLQ messages
4. **Backup**: Regular backups of Elasticsearch indices
5. **Retention**: Adjust ILM policy retention periods based on requirements
6. **Scaling**: Add more Kafka Connect workers for higher throughput
7. **Error Handling**: Monitor dead letter topics and set up alerting

