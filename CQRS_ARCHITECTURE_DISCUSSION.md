# CQRS in the Police System

## Introduction

This document contains a discussion of options for how to introduce CQRS to the application


## Context

We have built out a system that produces events for commands and observations using an edge server that produces events/commands to Kafka and NATS. 
This solution has no buisiness logic and it is time to figure out exactly what we'll do to introduce the CQRS projectsions.

## Alternaties for Discussion

### Introduction

We need a practical path to add CQRS projections without breaking the current edge pattern (REST in → `*Requested` events out, no state in edge). Below are the options, trade-offs, and the recommended hybrid.

### Option A: Event then query (poll)
*Keep the current edge behavior; clients poll read models once they exist.*
- Pros: Zero edge changes; pure event-driven; matches current rules.
- Cons: Clients do not know when processing is done; polling adds latency and client complexity.
- Use when: Latency is acceptable (dashboards, reports).

### Option B: NATS request/response + Kafka event (recommended default for commands needing fast acknowledgement)
*Edge keeps publishing `*Requested` to Kafka as source of truth; additionally calls BL over NATS request/response to give clients immediate acceptance feedback.*
- Pros: Better client experience (immediate accept/validation); reuses NATS; keeps Kafka authoritative.
- Cons: Two-path complexity; need strict contract so NATS reply reflects command acceptance, not final state.
- Use when: Commands need quick acknowledgement or validation (e.g., create/assign actions).

**Contract for Option B**
- NATS reply = command acceptance/validation result (e.g., 202 + `correlationId`), not final business outcome.
- Kafka remains the source of truth; always publish `*Requested` events with the same `correlationId`/`commandId`.
- If Kafka publish fails, fail the NATS reply. Add DLQ for Kafka failures.
- Include `correlationId` in REST response, NATS reply, and Kafka event for traceability.
- Idempotency: BL deduplicates on `commandId`.

**Flow for Option B**
1. Edge receives REST command.
2. Edge issues NATS request to the responsible BL service.
3. BL validates, publishes `*Requested` to Kafka (and optionally NATS fan-out), then replies on NATS with acceptance + `correlationId`.
4. Edge returns REST response (202 Accepted with `correlationId`), and clients can query later via read model using that id.

### Option C: Direct REST to projections/services
*Clients call projections/services directly; they publish events.*
- Pros: Fewer hops; clear ownership per service.
- Cons: Exposes CQRS topology in API; conflicts with current edge abstraction.
- Use when: We intentionally retire the edge layer and are ready to reshape APIs (not now).

## Recommended path
- Prefer **Option B** for commands needing immediate acknowledgement; keep Kafka as authoritative event stream.
- Use **Option A** for flows where eventual consistency is fine and latency is acceptable.
- Defer **Option C**; re-evaluate only if the edge stops adding value and we plan an API reshape.

## First increment (spike)
1. Pick one domain/command (e.g., `RegisterOfficer`).
2. Define request/response schema plus `correlationId` contract.
3. Write tests first: REST call → NATS R/R → verify Kafka `RegisterOfficerRequested` emitted (per existing test pattern).
4. Implement BL service with NATS listener, validation, Kafka publish, NATS reply.
5. Add a simple projection (Kafka Streams or consumer + in-memory store) for that domain and expose a query via edge.
6. Measure latency and error paths; then decide to expand.

## Projection tech choices (early)
- Start with Kafka Streams + state store (or simple consumer + in-memory) to keep rebuilds easy.
- Move to PostgreSQL for durable read models when needed; Mongo only if document flexibility is required.
- Support replay (consume from beginning) and DLQ for bad events.

## When to choose A vs B (rule of thumb)
- Needs immediate validation/ack: choose **B**.
- Tolerates eventual consistency/polling: choose **A**.
- Only consider **C** after deliberate API/edge deprecation decision.