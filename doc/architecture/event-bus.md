# Event Bus and Messaging Architecture

## Overview

The system implements a **double-publish pattern** using two event buses:
- **Apache Kafka**: Primary event bus and event store for event sourcing, long-term storage, and eventual consistency
- **NATS/JetStream**: Secondary event bus for critical messages requiring near realtime processing

All events are published to Kafka for event sourcing and long-term storage. Critical events (all command events ending with "Requested") are also published to NATS/JetStream for low-latency, near realtime processing.

## Infrastructure Setup

### Kafka Cluster

The system uses a 3-broker Kafka cluster:
- **Broker 1**: `kafka-broker-1:9092`
- **Broker 2**: `kafka-broker-2:9092`
- **Broker 3**: `kafka-broker-3:9092`
- **Kafka UI**: http://localhost:8080 (for management and monitoring)

Kafka is configured in KRaft mode (no Zookeeper required) with:
- Replication factor: 3
- Minimum in-sync replicas: 2
- High availability through broker replication

### NATS Cluster

The system uses a 3-node NATS cluster with JetStream enabled:
- **nats-1**: `localhost:4222` (primary client port), `localhost:8222` (monitoring)
- **nats-2**: `localhost:4223` (client port), `localhost:8223` (monitoring)
- **nats-3**: `localhost:4224` (client port), `localhost:8224` (monitoring)
- **Cluster Name**: `policesystem-nats-cluster`
- **NATS Tower**: http://localhost:8099 (web UI for cluster management)

The NATS cluster provides:
- **High Availability**: Automatic failover if a server goes down
- **JetStream**: Persistent messaging with at-least-once delivery guarantees
- **Low Latency**: Near realtime message delivery for critical events
- **Subject-Based Routing**: Events published to subjects like `commands.{domain}.{action}`

### Client Configuration

The application connects to all NATS servers for high availability:
```yaml
nats:
  url: nats://localhost:4222,nats://localhost:4223,nats://localhost:4224
  enabled: true
```

The NATS client automatically:
- Distributes connections across available servers
- Fails over to remaining servers if one goes down
- Reconnects automatically when servers come back online

## Kafka as Event Store

### Event Persistence

Kafka acts as the event store with the following characteristics:

- **Append-Only Log**: Events are appended to topics, never modified
- **Durability**: Events are persisted to disk and replicated
- **Ordering**: Events maintain order within partitions
- **Retention**: Events are retained according to retention policies
- **Replayability**: Events can be replayed from any point in time

### Event Storage Model

- **Topics**: Logical categories for events
- **Partitions**: Physical distribution of events within topics
- **Offsets**: Position markers for event consumption
- **Replication**: Events are replicated across brokers for durability

## Event Organization

### Topic Structure

Events are organized into topics based on:

- **Domain Boundaries**: Different domains may have separate topics
- **Event Types**: Different event types may be in separate topics
- **Aggregate Types**: Events for different aggregates may be partitioned
- **Performance Requirements**: High-volume events may need dedicated topics

### Partitioning Strategy

Events are partitioned to enable:

- **Parallel Processing**: Multiple consumers can process events in parallel
- **Ordering Guarantees**: Events within a partition maintain order
- **Scalability**: Partitions can be distributed across brokers
- **Load Distribution**: Events are distributed across partitions

### Partitioning Keys

Events are partitioned using keys:

- **Aggregate ID**: Ensures events for same aggregate are in same partition
- **Domain Entity**: Groups related events together
- **Geographic**: Distributes events by region
- **Time-based**: Distributes events by time windows

## NATS/JetStream for Critical Events

### Event Classification

Events are classified as critical or non-critical:
- **Critical Events**: All command events ending with "Requested" (e.g., `RegisterOfficerRequested`, `ReportIncidentRequested`)
- **Non-Critical Events**: Update events and other non-command events

### Subject Naming Convention

NATS JetStream uses subject-based routing with the pattern:
```
commands.{domain}.{action}
```

Examples:
- `RegisterOfficerRequested` → `commands.officer.register`
- `ReportIncidentRequested` → `commands.incident.report`
- `ChangeOfficerStatusRequested` → `commands.officer.change-status`
- `DispatchIncidentRequested` → `commands.incident.dispatch`

### JetStream Features

NATS JetStream provides:
- **At-Least-Once Delivery**: Messages are guaranteed to be delivered
- **Message Persistence**: Messages are stored on disk
- **Stream Processing**: Messages can be consumed from streams
- **Acknowledgment**: Messages are acknowledged after processing
- **Replayability**: Messages can be replayed from streams

### High Availability

The 3-node NATS cluster ensures:
- **Automatic Failover**: If one server fails, clients automatically connect to remaining servers
- **Cluster Replication**: JetStream metadata is replicated across all cluster nodes
- **Leader Election**: A leader is automatically elected for JetStream operations
- **Zero Downtime**: The cluster continues operating even if one server is down

### Monitoring

NATS provides several monitoring endpoints:
- **Health Check**: `http://localhost:8222/healthz`
- **Server Info**: `http://localhost:8222/varz`
- **Connections**: `http://localhost:8222/connz`
- **JetStream Info**: `http://localhost:8222/jsz`
- **NATS Tower**: Web UI at http://localhost:8099

## Event Consumption Patterns

### Consumer Groups

Different consumer groups serve different purposes:

1. **Projection Consumers**
   - Build and maintain read models
   - Process events to update projections
   - Maintain consistency within projections

2. **Integration Consumers**
   - Forward events to external systems
   - Transform events for external formats
   - Handle external system failures

3. **Analytics Consumers**
   - Process events for analytics
   - Build aggregations and summaries
   - Support reporting and dashboards

### Consumption Guarantees

- **At-Least-Once**: Events are delivered at least once
- **Ordering**: Events are processed in order within partitions
- **Offset Management**: Consumer offsets track processing progress
- **Idempotency**: Consumers handle duplicate events

## Kafka Streams for Projections

### Stream Processing

Kafka Streams is used for building CQRS projections:

- **Event Processing**: Processes events in real-time
- **State Management**: Maintains state for aggregations
- **Windowing**: Supports time-based windows for aggregations
- **Joining**: Can join events from multiple topics

### Stream Topology

Projections are built using stream topologies:

1. **Source**: Consume events from Kafka topics
2. **Transformation**: Transform events into projection format
3. **Aggregation**: Aggregate events into read model structures
4. **Sink**: Store projections in read model storage

### State Stores

Kafka Streams maintains state stores for:

- **Aggregations**: Running aggregations and summaries
- **Joins**: State for joining multiple event streams
- **Windows**: Time-windowed aggregations
- **Lookups**: Fast lookups for enrichment

## Event Schema

### Schema Evolution

Events support schema evolution:

- **Versioning**: Events have versions to support changes
- **Backward Compatibility**: New consumers can read old events
- **Forward Compatibility**: Old consumers can read new events (with defaults)
- **Schema Registry**: Centralized schema management (if used)

### Event Format

Events contain:

- **Metadata**: Timestamp, version, event ID, correlation ID
- **Aggregate Information**: Aggregate ID, aggregate type
- **Event Data**: Domain-specific event data
- **Causation**: Information about what caused the event

## Reliability and Guarantees

### Delivery Guarantees

- **At-Least-Once Delivery**: Events are delivered at least once
- **Ordering**: Events maintain order within partitions
- **Durability**: Events are persisted before acknowledgment
- **Replication**: Events are replicated across brokers

### Failure Handling

- **Broker Failure**: Replication ensures availability
- **Consumer Failure**: Consumers can resume from last offset
- **Network Failure**: Automatic retry and reconnection
- **Data Loss Prevention**: Replication and acknowledgments prevent data loss

## Performance Considerations

### Throughput

- **Partitioning**: More partitions enable higher throughput
- **Batching**: Events can be batched for efficiency
- **Compression**: Events can be compressed to reduce network usage
- **Parallel Processing**: Multiple consumers process events in parallel

### Latency

- **Batching**: Trade-off between latency and throughput
- **Replication**: Synchronous replication increases latency
- **Consumer Lag**: Monitor consumer lag to ensure timely processing
- **Network**: Optimize network configuration for low latency

## Monitoring and Operations

### Key Metrics

**Kafka Metrics:**
- **Throughput**: Events per second
- **Latency**: Time from event production to consumption
- **Consumer Lag**: Delay in event processing
- **Partition Distribution**: Even distribution across partitions
- **Replication**: Replication lag and health

**NATS Metrics:**
- **Connection Count**: Number of active connections
- **Message Throughput**: Messages per second
- **JetStream Storage**: Storage usage for streams
- **Cluster Health**: Status of all cluster nodes
- **Subject Activity**: Message activity per subject

### Operational Concerns

**Kafka:**
- **Topic Management**: Create, configure, and manage topics
- **Partition Management**: Add partitions for scaling
- **Consumer Management**: Monitor and manage consumer groups
- **Retention Policies**: Configure event retention
- **Resource Planning**: Plan for storage and network capacity

**NATS:**
- **Cluster Management**: Monitor cluster health and node status
- **Stream Management**: Create and manage JetStream streams
- **Subject Management**: Monitor subject activity and routing
- **Connection Management**: Monitor client connections
- **Failover Testing**: Test high availability by stopping nodes

### Admin UIs

- **Kafka UI**: http://localhost:8080 - Manage Kafka topics, consumers, and brokers
- **NATS Tower**: http://localhost:8099 - Manage NATS cluster, streams, and connections
- **NATS Monitoring**: Direct HTTP endpoints on ports 8222, 8223, 8224

### Testing High Availability

To test NATS cluster failover:
```bash
# Stop one NATS server
docker compose stop nats-2

# Verify cluster continues operating
curl http://localhost:8222/jsz

# Restart the server
docker compose start nats-2

# Verify it rejoins the cluster
curl http://localhost:8222/jsz
```

