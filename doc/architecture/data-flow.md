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

The edge service can query projections synchronously via NATS request-response for resource existence checks and conflict detection:

1. Edge receives REST command (e.g., `PUT /officers/{badgeNumber}`).
2. Edge validates request shape and required fields.
3. Edge sends NATS query request to projection (e.g., `query.officer.exists`).
4. Projection queries PostgreSQL read model.
5. Projection responds via NATS with existence status.
6. Edge uses response to:
   - Return 404 Not Found if resource doesn't exist (for update operations)
   - Return 409 Conflict if resource already exists (for create operations)
   - Proceed with command processing if resource exists
7. Edge publishes `*Requested` event to Kafka/NATS as before.

**Query Subject Pattern**: `query.{domain}.{operation}` (e.g., `query.officer.exists`, `query.call.exists`)

**Timeout**: Configurable timeout (default 2s) prevents hanging requests if projection is unavailable.

**Error Handling**: If projection query fails or times out, edge defaults to safe behavior (e.g., returns 404 for existence checks to prevent false positives).

## Consistency and Idempotency

- Kafka is authoritative for commands; if Kafka publish fails, the command is rejected.
- BL deduplicates on `commandId` to ensure idempotency.
- Eventual consistency between write path and read models; Option B only accelerates acceptance feedback, not state finality.

## Error Handling

- Validation failures return synchronously (via REST/NATS) with no Kafka publish.
- Kafka publish failures reject the command and record to DLQ.
- Projection failures retry; persistent issues route to DLQ for inspection/replay.

