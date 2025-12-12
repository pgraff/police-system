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

The officer projection (`officer-projection` module) serves as the reference implementation:

- **Module**: `officer-projection/`
- **Events**: `RegisterOfficerRequested`, `UpdateOfficerRequested`, `ChangeOfficerStatusRequested`
- **Topic**: `TopicConfiguration.OFFICER_EVENTS`
- **Tables**: `officer_projection`, `officer_status_history`
- **Endpoints**: `/api/projections/officers/{badgeNumber}`, `/api/projections/officers/{badgeNumber}/history`, `/api/projections/officers`
- **Deployment**: Standalone service (future K8s pod)

See the implementation for a complete working example.

## Implemented Projections

✅ **All 6 projection modules are complete:**

- ✅ **Officer Projection**: `officer-projection/` module → `officer-events` → `officer_projection` table
- ✅ **Incident Projection**: `incident-projection/` module → `incident-events` → `incident_projection` table
- ✅ **Call Projection**: `call-projection/` module → `call-events` → `call_projection` table
- ✅ **Dispatch Projection**: `dispatch-projection/` module → `dispatch-events` → `dispatch_projection` table
- ✅ **Activity Projection**: `activity-projection/` module → `activity-events` → `activity_projection` table
- ✅ **Assignment Projection**: `assignment-projection/` module → `assignment-events` → `assignment_projection` table

Each is a separate deployable service (future K8s pod).

## Future Projections (Optional)

Additional domains that could be projected:
- **Unit Projection**: `unit-projection/` module → `unit-events` → `unit_projection` table
- **Vehicle Projection**: `vehicle-projection/` module → `vehicle-events` → `vehicle_projection` table
- **Person Projection**: `person-projection/` module → `person-events` → `person_projection` table
- **Location Projection**: `location-projection/` module → `location-events` → `location_projection` table
- **Shift Projection**: `shift-projection/` module → `shift-events` → `shift_projection` table
