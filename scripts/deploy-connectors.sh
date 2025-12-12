#!/bin/bash

# Deploy Kafka Connect connectors for Elasticsearch indexing
# This script:
# 1. Waits for Kafka Connect to be ready
# 2. Creates dead letter topics
# 3. Applies Elasticsearch index templates and ILM policies
# 4. Registers all connectors via REST API
# 5. Verifies connector status

set -e

KAFKA_CONNECT_URL="${KAFKA_CONNECT_URL:-http://localhost:8083}"
KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"
ELASTICSEARCH_URL="${ELASTICSEARCH_URL:-http://elasticsearch:9200}"
ELASTICSEARCH_USERNAME="${ELASTICSEARCH_USERNAME:-}"
ELASTICSEARCH_PASSWORD="${ELASTICSEARCH_PASSWORD:-}"

CONNECTORS_DIR="${CONNECTORS_DIR:-docker/kafka-connect/connectors}"
TEMPLATES_DIR="${TEMPLATES_DIR:-docker/kafka-connect/elasticsearch/templates}"
POLICIES_DIR="${POLICIES_DIR:-docker/kafka-connect/elasticsearch/policies}"

# Dead letter topics (one per domain)
DLQ_TOPICS=(
  "dlq-officer-events"
  "dlq-vehicle-events"
  "dlq-unit-events"
  "dlq-person-events"
  "dlq-location-events"
  "dlq-incident-events"
  "dlq-call-events"
  "dlq-activity-events"
  "dlq-assignment-events"
  "dlq-shift-events"
  "dlq-officer-shift-events"
  "dlq-dispatch-events"
  "dlq-resource-assignment-events"
  "dlq-involved-party-events"
)

echo "=== Kafka Connect Elasticsearch Connector Deployment ==="
echo "Kafka Connect URL: $KAFKA_CONNECT_URL"
echo "Kafka Bootstrap Servers: $KAFKA_BOOTSTRAP_SERVERS"
echo "Elasticsearch URL: $ELASTICSEARCH_URL"
echo ""

# Function to wait for Kafka Connect to be ready
wait_for_connect() {
  echo "Waiting for Kafka Connect to be ready..."
  local max_attempts=30
  local attempt=1
  
  while [ $attempt -le $max_attempts ]; do
    if curl -sf "$KAFKA_CONNECT_URL/connectors" > /dev/null 2>&1; then
      echo "Kafka Connect is ready!"
      return 0
    fi
    echo "Attempt $attempt/$max_attempts: Kafka Connect not ready yet, waiting 5 seconds..."
    sleep 5
    attempt=$((attempt + 1))
  done
  
  echo "ERROR: Kafka Connect did not become ready within timeout"
  exit 1
}

# Function to create a Kafka topic
create_topic() {
  local topic=$1
  echo "Creating topic: $topic"
  
  docker exec kafka-broker-1 kafka-topics \
    --bootstrap-server localhost:9092 \
    --create \
    --topic "$topic" \
    --partitions 3 \
    --replication-factor 3 \
    --if-not-exists 2>/dev/null || {
    echo "Note: Topic $topic may already exist or creation failed (this is OK if it exists)"
  }
}

# Function to apply Elasticsearch ILM policy
apply_ilm_policy() {
  local policy_file=$1
  local policy_name=$(basename "$policy_file" .json)
  
  echo "Applying ILM policy: $policy_name"
  
  local auth=""
  if [ -n "$ELASTICSEARCH_USERNAME" ] && [ -n "$ELASTICSEARCH_PASSWORD" ]; then
    auth="-u $ELASTICSEARCH_USERNAME:$ELASTICSEARCH_PASSWORD"
  fi
  
  curl -X PUT "$ELASTICSEARCH_URL/_ilm/policy/$policy_name" \
    $auth \
    -H "Content-Type: application/json" \
    -d @"$policy_file" \
    -f -s > /dev/null || {
    echo "Warning: Failed to apply ILM policy $policy_name (Elasticsearch may not be available)"
  }
}

# Function to apply Elasticsearch index template
apply_index_template() {
  local template_file=$1
  local template_name=$(basename "$template_file" -template.json)
  
  echo "Applying index template: $template_name"
  
  local auth=""
  if [ -n "$ELASTICSEARCH_USERNAME" ] && [ -n "$ELASTICSEARCH_PASSWORD" ]; then
    auth="-u $ELASTICSEARCH_USERNAME:$ELASTICSEARCH_PASSWORD"
  fi
  
  curl -X PUT "$ELASTICSEARCH_URL/_index_template/$template_name" \
    $auth \
    -H "Content-Type: application/json" \
    -d @"$template_file" \
    -f -s > /dev/null || {
    echo "Warning: Failed to apply index template $template_name (Elasticsearch may not be available)"
  }
}

# Function to register a connector
register_connector() {
  local connector_file=$1
  local connector_name=$(jq -r '.name' "$connector_file")
  
  echo "Registering connector: $connector_name"
  
  # Substitute environment variables in connector config
  local config=$(envsubst < "$connector_file" | jq -c '.config')
  
  local response=$(curl -s -X POST "$KAFKA_CONNECT_URL/connectors" \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"$connector_name\", \"config\": $config}")
  
  if echo "$response" | jq -e '.error_code' > /dev/null 2>&1; then
    # Check if it's a "already exists" error
    if echo "$response" | jq -e '.error_code == 409' > /dev/null 2>&1; then
      echo "  Connector $connector_name already exists, updating..."
      curl -s -X PUT "$KAFKA_CONNECT_URL/connectors/$connector_name/config" \
        -H "Content-Type: application/json" \
        -d "$config" > /dev/null
    else
      echo "  ERROR: Failed to register connector: $response"
      return 1
    fi
  else
    echo "  Connector $connector_name registered successfully"
  fi
}

# Main execution
wait_for_connect

echo ""
echo "=== Step 1: Creating Dead Letter Topics ==="
for topic in "${DLQ_TOPICS[@]}"; do
  create_topic "$topic"
done

echo ""
echo "=== Step 2: Applying Elasticsearch ILM Policies ==="
if [ -d "$POLICIES_DIR" ]; then
  for policy_file in "$POLICIES_DIR"/*.json; do
    if [ -f "$policy_file" ]; then
      apply_ilm_policy "$policy_file"
    fi
  done
else
  echo "Warning: Policies directory not found: $POLICIES_DIR"
fi

echo ""
echo "=== Step 3: Applying Elasticsearch Index Templates ==="
if [ -d "$TEMPLATES_DIR" ]; then
  for template_file in "$TEMPLATES_DIR"/*.json; do
    if [ -f "$template_file" ]; then
      apply_index_template "$template_file"
    fi
  done
else
  echo "Warning: Templates directory not found: $TEMPLATES_DIR"
fi

echo ""
echo "=== Step 4: Registering Connectors ==="
if [ ! -d "$CONNECTORS_DIR" ]; then
  echo "ERROR: Connectors directory not found: $CONNECTORS_DIR"
  exit 1
fi

for connector_file in "$CONNECTORS_DIR"/*.json; do
  if [ -f "$connector_file" ]; then
    register_connector "$connector_file"
    sleep 1  # Small delay between registrations
  fi
done

echo ""
echo "=== Step 5: Verifying Connector Status ==="
sleep 5  # Give connectors time to start

all_healthy=true
for connector_file in "$CONNECTORS_DIR"/*.json; do
  if [ -f "$connector_file" ]; then
    connector_name=$(jq -r '.name' "$connector_file")
    status=$(curl -s "$KAFKA_CONNECT_URL/connectors/$connector_name/status" | jq -r '.connector.state // "UNKNOWN"')
    
    if [ "$status" = "RUNNING" ]; then
      echo "  ✓ $connector_name: $status"
    else
      echo "  ✗ $connector_name: $status"
      all_healthy=false
    fi
  fi
done

echo ""
if [ "$all_healthy" = true ]; then
  echo "=== Deployment Complete: All connectors are RUNNING ==="
  exit 0
else
  echo "=== Deployment Complete: Some connectors are not RUNNING ==="
  echo "Check connector status with: curl $KAFKA_CONNECT_URL/connectors/{connector-name}/status"
  exit 1
fi

