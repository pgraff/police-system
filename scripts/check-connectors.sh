#!/bin/bash

# Health check script for Kafka Connect connectors
# Verifies:
# - All connectors are running
# - No failed tasks
# - Events are being indexed (optional: check Elasticsearch)
# - Dead letter topics are empty (or log errors)

set -e

KAFKA_CONNECT_URL="${KAFKA_CONNECT_URL:-http://localhost:8083}"
KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"
ELASTICSEARCH_URL="${ELASTICSEARCH_URL:-http://elasticsearch:9200}"
ELASTICSEARCH_USERNAME="${ELASTICSEARCH_USERNAME:-}"
ELASTICSEARCH_PASSWORD="${ELASTICSEARCH_PASSWORD:-}"

CONNECTORS_DIR="${CONNECTORS_DIR:-docker/kafka-connect/connectors}"

# Dead letter topics
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

echo "=== Kafka Connect Connector Health Check ==="
echo "Kafka Connect URL: $KAFKA_CONNECT_URL"
echo ""

# Function to check connector status
check_connector_status() {
  local connector_name=$1
  local status_response=$(curl -s "$KAFKA_CONNECT_URL/connectors/$connector_name/status")
  local connector_state=$(echo "$status_response" | jq -r '.connector.state // "UNKNOWN"')
  local tasks=$(echo "$status_response" | jq -r '.tasks // []')
  
  echo "Connector: $connector_name"
  echo "  State: $connector_state"
  
  if [ "$connector_state" != "RUNNING" ]; then
    echo "  ⚠ WARNING: Connector is not RUNNING"
    return 1
  fi
  
  # Check tasks
  local task_count=$(echo "$tasks" | jq 'length')
  local failed_tasks=0
  
  for ((i=0; i<task_count; i++)); do
    local task_state=$(echo "$tasks" | jq -r ".[$i].state // \"UNKNOWN\"")
    local task_id=$(echo "$tasks" | jq -r ".[$i].id // \"$i\"")
    
    if [ "$task_state" != "RUNNING" ]; then
      echo "  ⚠ WARNING: Task $task_id is $task_state"
      failed_tasks=$((failed_tasks + 1))
    fi
  done
  
  if [ $failed_tasks -eq 0 ]; then
    echo "  ✓ All $task_count task(s) are RUNNING"
  else
    echo "  ✗ $failed_tasks out of $task_count task(s) are not RUNNING"
    return 1
  fi
  
  return 0
}

# Function to check dead letter topic
check_dlq_topic() {
  local topic=$1
  
  # Get message count (approximate)
  local count=$(docker exec kafka-broker-1 kafka-run-class kafka.tools.GetOffsetShell \
    --broker-list localhost:9092 \
    --topic "$topic" \
    --time -1 2>/dev/null | awk -F: '{sum += $3} END {print sum}' || echo "0")
  
  if [ "$count" = "0" ] || [ -z "$count" ]; then
    echo "  ✓ $topic: empty (no errors)"
  else
    echo "  ⚠ WARNING: $topic has $count message(s) - indexing errors detected!"
    echo "    Check messages with: docker exec -it kafka-broker-1 kafka-console-consumer --bootstrap-server localhost:9092 --topic $topic --from-beginning"
    return 1
  fi
  
  return 0
}

# Function to check Elasticsearch index (optional)
check_elasticsearch_index() {
  local index_pattern=$1
  
  local auth=""
  if [ -n "$ELASTICSEARCH_USERNAME" ] && [ -n "$ELASTICSEARCH_PASSWORD" ]; then
    auth="-u $ELASTICSEARCH_USERNAME:$ELASTICSEARCH_PASSWORD"
  fi
  
  local response=$(curl -s $auth "$ELASTICSEARCH_URL/$index_pattern/_count" 2>/dev/null || echo '{"count":0}')
  local count=$(echo "$response" | jq -r '.count // 0')
  
  if [ "$count" -gt 0 ]; then
    echo "  ✓ Index $index_pattern: $count document(s)"
  else
    echo "  ⚠ WARNING: Index $index_pattern: no documents (may be normal if no events yet)"
  fi
}

# Main execution
all_healthy=true

echo "=== Checking Connector Status ==="
if [ ! -d "$CONNECTORS_DIR" ]; then
  echo "ERROR: Connectors directory not found: $CONNECTORS_DIR"
  exit 1
fi

for connector_file in "$CONNECTORS_DIR"/*.json; do
  if [ -f "$connector_file" ]; then
    connector_name=$(jq -r '.name' "$connector_file")
    if ! check_connector_status "$connector_name"; then
      all_healthy=false
    fi
    echo ""
  fi
done

echo "=== Checking Dead Letter Topics ==="
dlq_errors=false
for topic in "${DLQ_TOPICS[@]}"; do
  if ! check_dlq_topic "$topic"; then
    dlq_errors=true
    all_healthy=false
  fi
done

echo ""
echo "=== Checking Elasticsearch Indices (optional) ==="
# Check a few sample indices
check_elasticsearch_index "officer-events-*"
check_elasticsearch_index "incident-events-*"
check_elasticsearch_index "call-events-*"

echo ""
if [ "$all_healthy" = true ] && [ "$dlq_errors" = false ]; then
  echo "=== Health Check: ALL SYSTEMS HEALTHY ==="
  exit 0
else
  echo "=== Health Check: ISSUES DETECTED ==="
  echo "Review the warnings above and check connector logs:"
  echo "  docker logs kafka-connect"
  exit 1
fi

