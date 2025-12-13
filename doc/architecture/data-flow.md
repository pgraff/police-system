# Data Flow Architecture

## Overview

The edge remains event-first and stateless: REST commands enter, `*Requested` events exit to Kafka. Business logic and projections live behind the edge. Two command paths exist—Kafka-only (Option A) and NATS request/response plus Kafka (Option B, default)—without exposing CQRS internals to clients.

## Command Flow (Option B – default)

1. Client sends REST command to the edge.
2. Edge performs shape/auth checks, then issues NATS request to the responsible BL service.
3. BL validates, publishes `*Requested` to Kafka (and optional NATS fan-out), and replies on NATS with acceptance plus `correlationId`.
4. Edge returns 202 Accepted with `correlationId`; Kafka remains the source of truth.

## Command Flow (Option A – Kafka-only)

1. Client sends REST command to the edge.
2. Edge performs shape/auth checks and publishes `*Requested` to Kafka with `correlationId`.
3. Edge returns 202 Accepted; clients poll projections/read models when available.

## Event Propagation and Projections

- Kafka stores `*Requested` events; DLQ protects failed publishes/consumes.
- ✅ **Implemented**: 6 projection services consume Kafka/NATS topics and build read models in PostgreSQL.
- ✅ **Technology**: Spring Kafka consumers (not Kafka Streams) for event consumption.
- ✅ **Storage**: PostgreSQL for all read models (durable, relational queries, joins).
- ✅ **Idempotency**: Event ID tracking prevents duplicate processing.
- Support replay from Kafka beginning; projection errors go to DLQ with retries.

## Query Flow

### Direct Projection Queries

1. Client issues query directly to projection service REST API (e.g., `/api/projections/officers/{badgeNumber}`).
2. Projection service queries PostgreSQL read model.
3. Projection service returns results to client.
4. Responses reflect eventual consistency; clients can correlate command→query via `correlationId` when provided.
5. (Future: Edge may route queries to projection services for unified API)

### Synchronous Projection Queries via NATS

The edge service can query projections synchronously via NATS request-response for resource existence checks and conflict detection. This enables the edge to validate resource existence before processing commands, providing immediate feedback to clients.

#### Query Flow Diagram

```
Client → Edge → NATS Request → Projection Query Handler → PostgreSQL → NATS Response → Edge → Client
```

#### Detailed Flow

1. **Edge receives REST command** (e.g., `PUT /officers/{badgeNumber}`).
2. **Edge validates request shape and required fields** (JSON structure, required fields present).
3. **Edge sends NATS query request** to projection using subject pattern `query.{domain}.{operation}` (e.g., `query.officer.exists`).
   - Request includes: `queryId`, `domain`, `resourceId`
   - Uses `NatsQueryClient` for synchronous request-response
4. **Projection query handler receives request** via NATS subscription.
5. **Projection queries PostgreSQL read model** to check resource existence.
6. **Projection responds via NATS** with `ExistsQueryResponse` containing existence status.
7. **Edge processes response**:
   - If resource doesn't exist → Return **404 Not Found** (for update operations)
   - If resource already exists → Return **409 Conflict** (for create operations)
   - If resource exists → Proceed with command processing
8. **Edge publishes `*Requested` event** to Kafka/NATS as before (if validation passes).

#### Query Subject Patterns

- **Existence queries**: `query.{domain}.exists` (e.g., `query.officer.exists`, `query.call.exists`, `query.incident.exists`)
- **Get queries**: `query.{domain}.get` (e.g., `query.officer.get`, `query.call.get`)

Supported domains: `officer`, `call`, `incident`, `dispatch`, `activity`, `assignment`

#### Timeout and Error Handling

- **Timeout**: Configurable timeout (default 2s) prevents hanging requests if projection is unavailable.
- **Connection failures**: If NATS connection fails, edge defaults to safe behavior.
- **Query failures**: If projection query fails or times out:
  - For existence checks: Edge defaults to `false` (returns 404) to prevent false positives
  - For conflict checks: Edge defaults to `false` (allows creation) to prevent false negatives
- **Projection unavailable**: If projection service is down or unreachable, queries timeout and edge handles gracefully.

#### Eventual Consistency Implications

Projections are eventually consistent. A resource may not appear in the projection immediately after creation. The edge queries projections synchronously, so:

- Recently created resources may return 404 if queried too quickly
- This is expected behavior in an eventually consistent system
- Clients should handle 404 responses appropriately and retry if needed
- The system prioritizes correctness over immediate availability

#### Example: Update Officer Flow

1. Client: `PUT /api/v1/officers/BADGE-123` with update data
2. Edge: Validates request structure
3. Edge → NATS: Sends `ExistsQueryRequest` to `query.officer.exists` with `resourceId=BADGE-123`
4. Projection: Queries PostgreSQL `officer_projection` table
5. Projection → NATS: Returns `ExistsQueryResponse(exists=true)`
6. Edge: Receives response, proceeds with update
7. Edge: Publishes `UpdateOfficerRequested` event to Kafka
8. Edge → Client: Returns `200 OK` with update confirmation

## Consistency and Idempotency

- Kafka is authoritative for commands; if Kafka publish fails, the command is rejected.
- BL deduplicates on `commandId` to ensure idempotency.
- Eventual consistency between write path and read models; Option B only accelerates acceptance feedback, not state finality.

## Error Handling

- Validation failures return synchronously (via REST/NATS) with no Kafka publish.
- Kafka publish failures reject the command and record to DLQ.
- Projection failures retry; persistent issues route to DLQ for inspection/replay.

