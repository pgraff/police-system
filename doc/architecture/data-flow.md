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

1. Client issues query directly to projection service REST API (e.g., `/api/projections/officers/{badgeNumber}`).
2. Projection service queries PostgreSQL read model.
3. Projection service returns results to client.
4. Responses reflect eventual consistency; clients can correlate command→query via `correlationId` when provided.
5. (Future: Edge may route queries to projection services for unified API)

## Consistency and Idempotency

- Kafka is authoritative for commands; if Kafka publish fails, the command is rejected.
- BL deduplicates on `commandId` to ensure idempotency.
- Eventual consistency between write path and read models; Option B only accelerates acceptance feedback, not state finality.

## Error Handling

- Validation failures return synchronously (via REST/NATS) with no Kafka publish.
- Kafka publish failures reject the command and record to DLQ.
- Projection failures retry; persistent issues route to DLQ for inspection/replay.

