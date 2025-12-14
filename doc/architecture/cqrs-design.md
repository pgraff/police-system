# CQRS Design

## Decision Summary

We keep the edge layer thin and event-first: REST commands in → `*Requested` events out to Kafka, with no state or business logic in the edge. CQRS projections are added behind the edge without changing this contract. We standardize on two interaction modes and avoid exposing CQRS details to clients.

## Interaction Options

- **Option A: Kafka-only (poll)** — Edge publishes `*Requested` to Kafka and returns 202; clients poll read models. Use when latency tolerance is acceptable (dashboards, reports).
- **Option B: NATS request/response + Kafka (default)** — Edge publishes `*Requested` to Kafka and also asks business logic (BL) over NATS for immediate acceptance feedback. Use when commands need quick validation/ack.
- **Option C: Direct-to-service** — Only if we deliberately retire the edge and reshape APIs (not current plan).

## Contract (applies to all options)

- Events are named as requests (`RegisterOfficerRequested`, etc.).
- `correlationId`/`commandId` travels through REST response, NATS reply, and Kafka event for traceability.
- Kafka is the source of truth. If Kafka publish fails, the command is rejected; add DLQ for failures.
- BL deduplicates on `commandId` for idempotency.

## Command Flow (Option B – default)

1. Edge receives REST command.
2. Edge issues NATS request to BL service.
3. BL validates, publishes `*Requested` to Kafka (and optionally NATS fan-out), and replies on NATS with acceptance + `correlationId`.
4. Edge returns 202 Accepted with `correlationId`. Clients can query projections later using the id.

## Command Flow (Option A – Kafka-only)

1. Edge receives REST command.
2. Edge validates shape, publishes `*Requested` to Kafka.
3. Edge returns 202 Accepted with `correlationId`. Clients poll read models when available.

## Query Side (Read Model)

- ✅ **Implemented**: Projections consume Kafka topics and build read models; they do not feed back into the edge contract.
- ✅ **Storage**: PostgreSQL is used for all read models (durable, relational queries, joins).
- ✅ **Technology**: Spring Kafka consumers (not Kafka Streams) for simplicity and flexibility.
- ✅ **Deployment**: Each projection is a separate standalone service (future K8s pod).
- ✅ **Modules**: 3 projection services implemented (operational-projection, resource-projection, workforce-projection).
- Support replay from Kafka and DLQ for bad events.

## Selection Rules (when to choose)

- Needs immediate validation/ack → choose **Option B**.
- Tolerates eventual consistency/polling → choose **Option A**.
- Only consider **Option C** if the edge is being retired.

## First Increment Pattern

1. Pick one command (e.g., `RegisterOfficer`).
2. Define request/response schema and `correlationId` contract.
3. Write tests first: REST call → NATS R/R (if Option B) → verify Kafka `*Requested` emitted.
4. Implement BL listener, validation, Kafka publish, NATS reply.
5. Add a simple projection and expose a query via the edge.
6. Measure latency/error paths, then expand to additional commands.

