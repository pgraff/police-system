# Component Architecture

## Overview

This document describes the abstract components of the system and their responsibilities. The architecture is composed of several key components that work together to implement Event Sourcing and CQRS.

## Component Diagram

```
┌─────────────┐
│   Clients   │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────┐
│         Edge Servers                │
│  ┌─────────────┐  ┌──────────────┐ │
│  │  Commands   │  │   Queries    │ │
│  └──────┬──────┘  └──────┬───────┘ │
│         │                │          │
└─────────┼────────────────┼──────────┘
          │                │
          ▼                │
┌─────────────────────────┼──────────┐
│      Event Bus (Kafka)              │
│  ┌──────────┐  ┌──────────────┐    │
│  │  Events  │  │   Topics     │    │
│  └────┬─────┘  └──────┬───────┘    │
└───────┼────────────────┼────────────┘
        │                │
        │                ▼
        │        ┌─────────────────────────────┐
        │        │  CQRS Projection Services    │
        │        │  (Spring Kafka Consumers)    │
        │        │  - operational-projection    │
        │        │  - resource-projection       │
        │        │  - workforce-projection      │
        │        └────────┬─────────────────────┘
        │                 │
        └─────────────────┘
```

## Edge Servers

### Purpose

Edge servers are the entry point to the system. They handle all client interactions and coordinate between commands and queries.

### Responsibilities

1. **Command Handling**
   - Receive commands from clients
   - Validate commands
   - Process business logic
   - Generate and publish events
   - Return responses to clients

2. **Query Handling**
   - **Direct Queries**: Projection services expose their own query APIs
   - **Synchronous Queries**: Edge queries projections via NATS request-response for resource existence checks
   - Each projection service handles queries for its domain
   - Future: Edge may route queries to projection services for unified API

3. **Resource Existence Checks**
   - Uses `ProjectionQueryService` to query projections synchronously via NATS
   - Enables 404 Not Found responses for non-existent resources
   - Enables 409 Conflict responses for duplicate resources
   - Queries use subject pattern: `query.{domain}.exists`

3. **API Gateway**
   - Provide unified REST API
   - Handle authentication and authorization
   - Rate limiting and throttling
   - Request/response transformation

### Technology

- **Framework**: Spring Framework
- **Language**: Java 17
- **Deployment**: Stateless, horizontally scalable

### Characteristics

- **Stateless**: No session state stored in edge servers
- **Horizontally Scalable**: Multiple instances can run in parallel
- **Resilient**: Failures in one instance don't affect others
- **Load Balanced**: Requests distributed across instances

## Event Bus (Kafka)

### Purpose

Kafka serves as the central event bus and event store for the system. All events flow through Kafka.

### Responsibilities

1. **Event Storage**
   - Persist all domain events
   - Maintain event ordering
   - Provide event replay capabilities
   - Ensure durability and replication

2. **Event Distribution**
   - Distribute events to consumers
   - Support multiple consumer groups
   - Handle event routing via topics
   - Manage consumer offsets

3. **Scalability**
   - Handle high throughput
   - Scale horizontally
   - Partition events for parallel processing
   - Retain events according to policies

### Topics Structure

Events are organized into topics:

- **Domain Events**: Events representing business occurrences
- **Command Events**: Events representing command execution
- **Integration Events**: Events for external system integration

### Characteristics

- **Durability**: Events are persisted to disk
- **Ordering**: Events maintain order within partitions
- **Replayability**: Events can be replayed from any offset
- **Scalability**: Handles millions of events per second

## NATS/JetStream

### Purpose

NATS/JetStream serves two roles in the system:
1. **Event Distribution**: Critical events are published to NATS for near realtime processing
2. **Request-Response**: Synchronous queries from edge to projections for resource existence checks

### Responsibilities

1. **Event Distribution**
   - Publish critical events (all `*Requested` events) to NATS subjects
   - Enable low-latency event processing
   - Support JetStream for persistence and at-least-once delivery

2. **Request-Response Queries**
   - Handle synchronous query requests from edge to projections
   - Subject pattern: `query.{domain}.{operation}` (e.g., `query.officer.exists`)
   - Enable immediate resource existence checks
   - Support timeout configuration for reliability

### Subject Patterns

- **Command Events**: `commands.{domain}.{action}` (e.g., `commands.officer.register`)
- **Query Requests**: `query.{domain}.{operation}` (e.g., `query.officer.exists`)

### Characteristics

- **Low Latency**: Near realtime message delivery
- **High Availability**: 3-node cluster with automatic failover
- **Request-Response**: Built-in support for synchronous queries
- **Persistence**: JetStream provides at-least-once delivery guarantees

## NATS Query Infrastructure Components

### NatsQueryClient

**Location**: `common` module  
**Purpose**: Client for sending synchronous query requests to projections via NATS request-response pattern.

**Responsibilities**:
- Establish and manage NATS connection
- Send query requests to projection services
- Wait for responses with configurable timeout
- Handle connection failures and timeouts
- Support both synchronous and asynchronous query methods
- Support single server and multi-server (cluster) configurations

**Configuration**:
- `nats.url`: NATS server URL(s) (comma-separated for clusters)
- `nats.query.timeout`: Query timeout in milliseconds (default: 2000ms)
- `nats.query.enabled`: Enable/disable query client (default: true)

**Error Handling**:
- Throws `NatsQueryException` on failures
- Handles timeouts gracefully
- Supports disabled state for testing

### ProjectionQueryService

**Location**: `edge` module  
**Purpose**: Service layer for querying projections via NATS. Provides high-level methods for existence checks and resource retrieval.

**Responsibilities**:
- Abstract NATS query details from edge services
- Construct correct subject patterns (`query.{domain}.exists`, `query.{domain}.get`)
- Handle query failures and exceptions
- Provide domain-specific query methods

**Methods**:
- `exists(String domain, String resourceId)`: Check if resource exists
- `get(String domain, String resourceId)`: Get resource data

**Error Handling**:
- Throws `ProjectionQueryException` on failures
- Wraps `NatsQueryException` with context

### Existence Services

**Location**: `edge` module, domain-specific packages  
**Purpose**: Domain-specific services for checking resource existence in projections.

**Services**:
- `OfficerExistenceService`: Check officer existence by badge number
- `OfficerConflictService`: Check for duplicate badge numbers
- `CallExistenceService`: Check call existence by call ID
- `IncidentExistenceService`: Check incident existence by incident ID
- `ActivityExistenceService`: Check activity existence by activity ID
- `AssignmentExistenceService`: Check assignment existence by assignment ID
- `DispatchExistenceService`: Check dispatch existence by dispatch ID

**Responsibilities**:
- Provide domain-specific existence check methods
- Handle query failures gracefully (default to safe values)
- Log errors for debugging

**Error Handling**:
- Default to `false` on query failures (safe defaults)
- Log errors for monitoring
- Return appropriate boolean values for existence checks

### Projection Query Handlers

**Location**: Each projection module (`{domain}-projection`)  
**Purpose**: Handle synchronous query requests from edge via NATS.

**Components**:
- `{Domain}NatsQueryHandler`: Handles NATS query requests for each projection domain

**Responsibilities**:
- Subscribe to NATS query subjects (`query.{domain}.exists`, `query.{domain}.get`)
- Query PostgreSQL read model
- Respond with `ExistsQueryResponse` or `GetQueryResponse`
- Handle errors and return error responses

**Query Types**:
- **Exists queries**: Check if resource exists in projection
- **Get queries**: Retrieve full resource data from projection

**Subject Patterns**:
- `query.officer.exists`, `query.officer.get`
- `query.call.exists`, `query.call.get`
- `query.incident.exists`, `query.incident.get`
- `query.dispatch.exists`, `query.dispatch.get`
- `query.activity.exists`, `query.activity.get`
- `query.assignment.exists`, `query.assignment.get`

## CQRS Projections

### Purpose

CQRS projections build and maintain read models from events, enabling efficient query operations.

### Responsibilities

1. **Event Consumption**
   - Consume events from Kafka
   - Process events in order
   - Handle event replay
   - Manage consumer offsets

2. **Read Model Building**
   - Transform events into read model structures
   - Update read models incrementally
   - Maintain consistency within read models
   - Handle schema evolution

3. **Query Support**
   - Maintain indexes for fast queries
   - Support various query patterns
   - Aggregate data for reporting
   - Provide search capabilities
   - **NATS Query Handlers**: Handle synchronous query requests from edge via NATS
   - Respond to existence checks (`query.{domain}.exists`) for resource validation

### Technology

- **Framework**: Spring Boot
- **Event Consumption**: Spring Kafka consumers (not Kafka Streams)
- **Storage**: PostgreSQL for read models
- **Language**: Java 17
- **Deployment**: Standalone services (future K8s pods)

### Characteristics

- **Asynchronous**: Processes events asynchronously
- **Eventually Consistent**: Read models lag behind writes
- **Scalable**: Can scale independently from command side
- **Resilient**: Can recover from failures by replaying events

### Implemented Projections

✅ **3 projection services fully implemented:**

1. **Operational Projection** (`operational-projection`)
   - Handles: incidents, calls, dispatches, activities, assignments, involved parties, resource assignments
   - Query endpoints: Get by ID, list with filters, status history for all operational entities
   - Deployment: Standalone service (Port 8081, future K8s pod)

2. **Resource Projection** (`resource-projection`)
   - Handles: officers, vehicles, units, persons, locations
   - Query endpoints: Get by ID, list with filters, status history for all resource entities
   - Deployment: Standalone service (Port 8082, future K8s pod)

3. **Workforce Projection** (`workforce-projection`)
   - Handles: shifts, officer shifts, shift changes
   - Query endpoints: Get by ID, list with filters, status history for all workforce entities
   - Deployment: Standalone service (Port 8083, future K8s pod)

### Future Projection Types (Optional)

1. **Aggregations**
   - Summary statistics
   - Incremental updates
   - Used for dashboards

2. **Search Indexes**
   - Full-text search
   - Fast lookup capabilities
   - Updated from events

## Component Interactions

### Command Flow

1. Client sends command to edge server
2. Edge server validates and processes command
3. Edge server publishes events to Kafka
4. Kafka stores events and distributes to consumers
5. Projections consume events and update read models
6. Edge server returns response to client

### Query Flow

#### Direct Projection Queries

1. Client sends query directly to projection service API
2. Projection service queries PostgreSQL read model
3. Projection service returns results to client
4. (Future: Edge may route queries to projection services)

#### Synchronous Edge Queries (via NATS)

1. Edge receives REST command that requires resource existence check
2. Edge sends NATS query request to projection (e.g., `query.officer.exists`)
3. Projection queries PostgreSQL read model
4. Projection responds via NATS with existence status
5. Edge uses response to return appropriate HTTP status (404/409) or proceed with command
6. Edge publishes `*Requested` event to Kafka/NATS as before

### Event Flow

1. Events published to Kafka
2. Kafka distributes events to all consumer groups
3. Projections consume events and update read models
4. Other systems can consume events for integration

## Scalability Considerations

### Horizontal Scaling

- **Edge Servers**: Scale by adding more instances
- **Kafka**: Scale by adding more brokers and partitions
- **Projections**: Scale by adding more consumer instances

### Vertical Scaling

- Each component can be scaled vertically by increasing resources
- Kafka partitions can be increased for better parallelism
- Read models can be optimized for specific query patterns

## Resilience

### Failure Handling

- **Edge Server Failure**: Other instances continue serving requests
- **Kafka Failure**: Events are replicated across brokers
- **Projection Failure**: Can replay events to recover state

### Recovery

- **Event Replay**: System can replay events to recover
- **State Reconstruction**: State can be rebuilt from events
- **Read Model Rebuild**: Projections can rebuild from scratch

