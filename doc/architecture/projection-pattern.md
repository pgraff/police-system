# CQRS Projection Implementation Pattern

This document describes the established pattern for implementing CQRS projections in the police system. The officer projection (`officer-projection` module) serves as the reference implementation.

## Architecture Overview

**Each projection is a separate deployable service** (future K8s pod). This means:
- `officer-projection/` - standalone service for officer read models
- `incident-projection/` - standalone service for incident read models (future)
- `dispatch-projection/` - standalone service for dispatch read models (future)
- Each service can be scaled, deployed, and monitored independently

## Overview

A CQRS projection service:
1. **Consumes events** from Kafka (and optionally NATS) for a specific domain
2. **Maintains read models** in PostgreSQL for efficient querying
3. **Exposes query APIs** for read-only access to projected data
4. **Handles idempotency** to safely replay events

## Architecture Layers

### 1. Event Consumers (`consumer/` and `nats/`)

**Kafka Consumer** (`*KafkaListener.java`):
- Subscribes to domain-specific Kafka topic (e.g., `TopicConfiguration.OFFICER_EVENTS`)
- Uses `@KafkaListener` with manual acknowledgment
- Parses JSON events using `*EventParser`
- Delegates to projection service
- Acknowledges after processing (relies on idempotency for error handling)

**NATS Consumer** (`*NatsListener.java`):
- Optional: subscribes to NATS subjects (e.g., `commands.officer.>`)
- Handles duplicate events safely (Kafka is source of truth)
- Uses dispatcher pattern for async message handling

**Event Parser** (`*EventParser.java`):
- Parses JSON string payloads into typed event objects
- Uses heuristics (field presence, subject hints) to determine event type
- Handles polymorphic event deserialization

### 2. Projection Service (`service/`)

**Service** (`*ProjectionService.java`):
- Central handler for all domain events
- Routes events to specific handler methods
- Coordinates repository operations
- Provides query methods for API layer
- Uses `Clock` for timestamp generation (testable)

**Event Handlers**:
- `handleRegister()` - Initial creation of projection row
- `handleUpdate()` - Partial updates to existing row
- `handleChangeStatus()` - Status changes with history tracking
- Each handler is idempotent (safe to replay)

### 3. Repository (`repository/`)

**Repository** (`*ProjectionRepository.java`):
- JDBC-based data access using `JdbcTemplate`
- Implements upsert patterns for idempotency
- Handles partial updates (null fields preserved)
- Manages history tables for audit trails
- Converts between `Instant` and `Timestamp` for database

**Key Patterns**:
- `upsert()` - INSERT ... ON CONFLICT DO UPDATE
- `applyUpdate()` - Merge partial updates with existing state
- `changeStatus()` - Update status + append history only when changed
- Query methods with filtering and pagination

### 4. Data Models (`model/`)

**Entity Records**:
- `*ProjectionEntity` - Main projection table record
- `*HistoryEntry` - History/audit trail records
- Use Java records for immutability

### 5. API Layer (`api/`)

**Controller** (`*ProjectionController.java`):
- REST endpoints under `/api/projections/{domain}/`
- `GET /{id}` - Single entity lookup
- `GET /{id}/history` - History/audit trail
- `GET /` - List with filters and pagination
- Returns 404 for missing entities

**Response DTOs**:
- `*ProjectionResponse` - Single entity response
- `*HistoryResponse` - History entry response
- `*ProjectionPageResponse` - Paginated list response

### 6. Configuration (`config/`)

**ProjectionConfig**:
- Kafka consumer factory configuration
- ObjectMapper with JavaTimeModule
- Clock bean for timestamp generation

**NatsProperties**:
- Configuration properties for NATS connection
- Subject patterns and enable flags

## Database Schema

### Main Projection Table
```sql
CREATE TABLE {domain}_projection (
    {id_column} VARCHAR PRIMARY KEY,
    -- domain-specific fields
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### History Table (if needed)
```sql
CREATE TABLE {domain}_status_history (
    id BIGSERIAL PRIMARY KEY,
    {id_column} VARCHAR NOT NULL REFERENCES {domain}_projection({id_column}),
    status VARCHAR NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

**Indexes**:
- Primary key on `{id_column}`
- Indexes on frequently queried fields
- Composite indexes for filter combinations

## Testing Pattern

### Integration Tests (`integration/`)

**Base Class** (`IntegrationTestBase`):
- Sets up Testcontainers for Kafka, NATS, PostgreSQL
- Configures Spring Boot test context
- Provides shared container instances

**Test Class** (`*ProjectionIntegrationTest`):
- Publishes events to Kafka using producer
- Verifies projection state via JDBC queries
- Tests query endpoints using `TestRestTemplate`
- Uses `Awaitility` for async assertions
- Tests idempotency (duplicate events)
- Tests NATS/Kafka duplicate handling

**Test Scenarios**:
1. Register event → projection row created
2. Update event → partial fields updated, others preserved
3. Status change → status updated + history appended
4. Duplicate events → no corruption (idempotency)
5. Query endpoints → correct responses

## Implementation Checklist

When implementing a new projection:

### 1. Schema
- [ ] Create projection table DDL in `schema.sql`
- [ ] Create history table (if needed)
- [ ] Add indexes for query patterns

### 2. Models
- [ ] Create `*ProjectionEntity` record
- [ ] Create `*HistoryEntry` record (if needed)
- [ ] Create response DTOs

### 3. Repository
- [ ] Implement `upsert()` for register events
- [ ] Implement `applyUpdate()` for update events
- [ ] Implement `changeStatus()` if status tracking needed
- [ ] Implement query methods (`findBy*`, `findAll`, `count`)
- [ ] Add `ts()` helper for Instant→Timestamp conversion

### 4. Service
- [ ] Create service class with `handle()` method
- [ ] Implement event-specific handlers
- [ ] Implement query methods
- [ ] Add entity→DTO mapping

### 5. Consumers
- [ ] Create `*EventParser` for JSON deserialization
- [ ] Create `*KafkaListener` with `@KafkaListener`
- [ ] Create `*NatsListener` (optional)
- [ ] Wire to projection service

### 6. API
- [ ] Create controller with query endpoints
- [ ] Implement single entity lookup
- [ ] Implement history endpoint (if needed)
- [ ] Implement list endpoint with filters/pagination

### 7. Configuration
- [ ] Add NATS properties (if using NATS)
- [ ] Verify Kafka consumer config
- [ ] Add Clock bean if not already present

### 8. Tests
- [ ] Create integration test class
- [ ] Test register → projection created
- [ ] Test update → partial update works
- [ ] Test status change → history appended
- [ ] Test idempotency
- [ ] Test query endpoints

## Key Principles

1. **Idempotency**: All operations must be safe to replay
   - Use `ON CONFLICT DO UPDATE` for upserts
   - Only append history when state actually changes
   - Check current state before applying updates

2. **Event-Driven**: Projections react to events, never initiate commands
   - No business logic in projections
   - No event publishing from projections
   - Pure read model building

3. **Eventually Consistent**: Read models lag behind writes
   - Tests use `Awaitility` for async assertions
   - APIs may return stale data briefly
   - Design for this latency

4. **Separation of Concerns**:
   - Consumers handle event ingestion
   - Service coordinates business logic
   - Repository handles persistence
   - API handles HTTP concerns

5. **Testability**:
   - Use `Clock` for timestamps (mockable)
   - Integration tests with Testcontainers
   - Verify both DB state and API responses

## Example: Officer Projection

## Consolidated Projections (Current)

The system uses 3 consolidated projection services organized by use case:

### Operational Projection

- **Module**: `operational-projection/`
- **Handles**: incidents, calls, dispatches, activities, assignments, involved parties, resource assignments
- **Events**: All operational domain events
- **Topics**: `incident-events`, `call-events`, `dispatch-events`, `activity-events`, `assignment-events`, `involved-party-events`, `resource-assignment-events`
- **Tables**: `incident_projection`, `call_projection`, `dispatch_projection`, `activity_projection`, `assignment_projection`, `involved_party_projection`, `resource_assignment_projection`, and status history tables
- **Endpoints**: `/api/projections/incidents/*`, `/api/projections/calls/*`, `/api/projections/dispatches/*`, `/api/projections/activities/*`, `/api/projections/assignments/*`, etc.
- **Deployment**: Standalone service (Port 8081, future K8s pod)

### Resource Projection

- **Module**: `resource-projection/`
- **Handles**: officers, vehicles, units, persons, locations
- **Events**: All resource/master data events
- **Topics**: `officer-events`, `vehicle-events`, `unit-events`, `person-events`, `location-events`
- **Tables**: `officer_projection`, `vehicle_projection`, `unit_projection`, `person_projection`, `location_projection`, and status history tables
- **Endpoints**: `/api/projections/officers/*`, `/api/projections/vehicles/*`, `/api/projections/units/*`, `/api/projections/persons/*`, `/api/projections/locations/*`
- **Deployment**: Standalone service (Port 8082, future K8s pod)

### Workforce Projection

- **Module**: `workforce-projection/`
- **Handles**: shifts, officer shifts, shift changes
- **Events**: All workforce/shift management events
- **Topics**: `shift-events`, `officer-shift-events`
- **Tables**: `shift_projection`, `officer_shift_projection`, `shift_change_projection`, `shift_status_history`
- **Endpoints**: `/api/projections/shifts/*`, `/api/projections/officer-shifts/*`, `/api/projections/shift-changes/*`
- **Deployment**: Standalone service (Port 8083, future K8s pod)

## Legacy Individual Projections (Deprecated)

⚠️ **DEPRECATED**: The following individual projection modules are deprecated and will be removed in a future release:

- ⚠️ **Officer Projection**: `officer-projection/` → Use `resource-projection` instead
- ⚠️ **Incident Projection**: `incident-projection/` → Use `operational-projection` instead
- ⚠️ **Call Projection**: `call-projection/` → Use `operational-projection` instead
- ⚠️ **Dispatch Projection**: `dispatch-projection/` → Use `operational-projection` instead
- ⚠️ **Activity Projection**: `activity-projection/` → Use `operational-projection` instead
- ⚠️ **Assignment Projection**: `assignment-projection/` → Use `operational-projection` instead

**Migration Guide**: See [Client Migration Guide](../migration/client-migration-guide.md) for details.

**Note**: These modules are kept in the codebase during the migration period but are marked as deprecated. They will be removed after successful migration.
