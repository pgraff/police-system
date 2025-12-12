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
        │        │  - officer-projection        │
        │        │  - incident-projection       │
        │        │  - call-projection           │
        │        │  - dispatch-projection       │
        │        │  - activity-projection       │
        │        │  - assignment-projection     │
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

2. **Query Handling** (Note: Currently handled by projection services directly)
   - Projection services expose their own query APIs
   - Each projection service handles queries for its domain
   - Future: Edge may route queries to projection services

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

✅ **6 projection modules fully implemented:**

1. **Officer Projection** (`officer-projection`)
   - Handles officer registration, updates, status changes
   - Query endpoints: Get by badge, list with filters, status history

2. **Incident Projection** (`incident-projection`)
   - Handles all incident events (report, dispatch, arrive, clear, update, status change)
   - Query endpoints: Get by ID, list with filters, status history

3. **Call Projection** (`call-projection`)
   - Handles all call events (receive, dispatch, arrive, clear, update, status change)
   - Query endpoints: Get by ID, list with filters, status history

4. **Dispatch Projection** (`dispatch-projection`)
   - Handles dispatch creation and status changes
   - Query endpoints: Get by ID, list with filters, status history

5. **Activity Projection** (`activity-projection`)
   - Handles activity start, update, status change, completion
   - Query endpoints: Get by ID, list with filters, status history

6. **Assignment Projection** (`assignment-projection`)
   - Handles assignment creation, status changes, completion, dispatch linking, resource assignment
   - Query endpoints: Get by ID, list with filters, status history, resource assignments

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

1. Client sends query directly to projection service API
2. Projection service queries PostgreSQL read model
3. Projection service returns results to client
4. (Future: Edge may route queries to projection services)

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

