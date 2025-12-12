# Architecture Overview

## Introduction

This overview reflects the adopted CQRS approach from `CQRS_ARCHITECTURE_DISCUSSION.md`: the edge is thin and event-first (REST in → `*Requested` events out to Kafka), with business logic and projections behind the edge. We support two command interaction modes—Kafka-only polling and NATS request/response plus Kafka—without exposing CQRS internals to clients.

## Core Architectural Patterns

### Event Sourcing

We record changes as events in Kafka. State can be replayed from events, supporting auditability and rebuilds of projections/read models.

### CQRS (Command Query Responsibility Segregation)

Commands and queries are split. Commands enter via the edge and are expressed as `*Requested` events; queries read from projections fed by Kafka. Acceptance feedback can be provided over NATS (Option B) while Kafka remains authoritative.

## System Components

1. **Edge Servers**: Authenticate/authorize requests, publish `*Requested` events, optionally request/response over NATS for immediate acceptance.
2. **Event Bus (Kafka)**: Source of truth for command events, supporting replay and DLQ.
3. **NATS (request/response)**: Optional fast-path validation/ack channel paired with Kafka publish.
4. **CQRS Projections**: ✅ **6 standalone projection services** consume Kafka/NATS to build read models in PostgreSQL. Each projection is a separate deployable service with its own query APIs.

## Technology Stack

- **Language**: Java 17
- **Framework**: Spring Framework / Spring Boot
- **Event Bus**: Apache Kafka
- **Request/Response**: NATS/JetStream
- **Projections**: Spring Kafka consumers (6 standalone projection services)
- **Read Model Storage**: PostgreSQL

## Document Structure

- [Architecture Overview](overview.md) (this document)
- [Event Sourcing](event-sourcing.md)
- [CQRS Design](cqrs-design.md)
- [Component Architecture](components.md)
- [Event Bus and Messaging](event-bus.md)
- [Data Flow](data-flow.md)

