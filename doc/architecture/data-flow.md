# Data Flow Architecture

## Overview

This document describes how data flows through the system, from command reception to query serving, and how events propagate through the system.

## Command Flow

### 1. Command Reception

```
Client → Edge Server (REST API)
```

- Client sends HTTP request with command payload
- Edge server receives request at REST endpoint
- Request is authenticated and authorized
- Command is extracted from request body

### 2. Command Validation

```
Edge Server → Business Logic → Validation Result
```

- Command syntax is validated
- Business rules are checked
- Current state may be loaded (if needed)
- Validation errors are returned if command is invalid

### 3. Event Generation

```
Edge Server → Domain Logic → Domain Events
```

- Business logic processes valid command
- Domain events are generated
- Events represent facts that occurred
- Events are immutable and timestamped

### 4. Event Publishing

```
Edge Server → Kafka → Event Store
```

- Events are published to appropriate Kafka topics
- Events are partitioned based on aggregate ID
- Kafka acknowledges successful storage
- Events are replicated across brokers

### 5. Response to Client

```
Edge Server → Client (HTTP Response)
```

- Success response is returned to client
- Response may include generated IDs or confirmation
- Error responses are returned for failures
- Response is sent before events are fully processed

## Event Propagation Flow

### 1. Event Storage

```
Kafka Topic → Partitions → Replication
```

- Events are stored in Kafka topics
- Events are partitioned for scalability
- Events are replicated for durability
- Events are retained according to retention policy

### 2. Event Distribution

```
Kafka → Consumer Groups → Consumers
```

- Kafka distributes events to consumer groups
- Each consumer group processes events independently
- Multiple consumers in a group process events in parallel
- Consumer offsets track processing progress

### 3. Projection Processing

```
Kafka → Kafka Streams → Projection Logic → Read Model
```

- Kafka Streams consumes events from topics
- Events are processed by projection logic
- Read models are updated based on events
- State stores maintain intermediate state

### 4. Read Model Update

```
Projection → Read Model Storage
```

- Projections update read model storage
- Updates are incremental (event-driven)
- Read models are optimized for queries
- Multiple read models may be updated from same events

## Query Flow

### 1. Query Reception

```
Client → Edge Server (REST API)
```

- Client sends HTTP request with query parameters
- Edge server receives request at REST endpoint
- Request is authenticated and authorized
- Query parameters are extracted

### 2. Query Routing

```
Edge Server → Query Handler → Read Model
```

- Edge server routes query to appropriate handler
- Handler determines which read model to query
- Query may span multiple read models
- Query is optimized for the read model structure

### 3. Read Model Query

```
Query Handler → Read Model Storage → Results
```

- Query is executed against read model
- Read model is optimized for query pattern
- Results are retrieved efficiently
- Results may be aggregated or transformed

### 4. Response to Client

```
Edge Server → Client (HTTP Response)
```

- Query results are returned to client
- Results are formatted as JSON (or other format)
- Pagination may be applied for large result sets
- Response includes metadata if needed

## Event Replay Flow

### 1. Replay Initiation

```
System/Operator → Kafka → Event Stream
```

- Replay is initiated for recovery or migration
- Starting offset is determined
- Target consumer group is configured
- Replay parameters are set

### 2. Event Consumption

```
Kafka → Consumer → Events (Historical)
```

- Consumer reads events from specified offset
- Events are consumed in order
- All events are processed, not just new ones
- Consumer processes events at configured rate

### 3. State Reconstruction

```
Events → Projection Logic → State
```

- Events are processed to reconstruct state
- State is built incrementally from events
- Historical state can be reconstructed at any point
- State is validated during reconstruction

### 4. Read Model Rebuild

```
Reconstructed State → Read Model Storage
```

- Read models are rebuilt from reconstructed state
- Old read models may be replaced
- New read models may be created
- Rebuild completes when all events are processed

## Integration Flow

### 1. External Event Consumption

```
External System → Kafka → Integration Consumer
```

- External systems may publish events to Kafka
- Integration consumers process external events
- Events are validated and transformed
- Events are integrated into system

### 2. External Event Publishing

```
System Events → Kafka → External Consumer
```

- System events are published to Kafka
- External systems consume relevant events
- Events are transformed for external format
- External systems process events asynchronously

## Consistency Flow

### 1. Write Consistency

```
Command → Event → Acknowledgment
```

- Commands are processed synchronously
- Events are published atomically
- Acknowledgment confirms event storage
- Consistency is guaranteed within aggregates

### 2. Read Consistency

```
Event → Projection → Read Model → Query
```

- Events are processed asynchronously
- Projections update read models eventually
- Queries read from eventually consistent models
- Consistency lag is typically small

### 3. Eventual Consistency

```
Write → Event → Projection → Read
```

- Writes are immediately consistent
- Events propagate asynchronously
- Projections catch up with writes
- Reads eventually see all writes

## Error Flow

### 1. Command Errors

```
Command → Validation → Error Response
```

- Invalid commands return errors immediately
- Business rule violations return errors
- Errors are returned synchronously
- No events are generated for errors

### 2. Event Processing Errors

```
Event → Projection → Error → Retry/DLQ
```

- Projection errors are handled gracefully
- Failed events may be retried
- Dead letter queues handle persistent failures
- Errors are logged and monitored

### 3. Query Errors

```
Query → Read Model → Error Response
```

- Query errors return appropriate HTTP status
- Errors are logged for debugging
- Client receives error details
- System continues operating despite errors

## Performance Flow

### 1. Command Throughput

```
Commands → Edge Servers (Parallel) → Kafka
```

- Multiple edge servers process commands in parallel
- Commands are load balanced across servers
- Kafka handles high throughput
- System scales horizontally

### 2. Event Processing Throughput

```
Events → Partitions → Consumers (Parallel)
```

- Events are partitioned for parallel processing
- Multiple consumers process events concurrently
- Kafka Streams processes streams in parallel
- System scales with partitions and consumers

### 3. Query Throughput

```
Queries → Edge Servers (Parallel) → Read Models
```

- Multiple edge servers serve queries in parallel
- Read models are optimized for queries
- Queries are load balanced
- Caching may improve query performance

