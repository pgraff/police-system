# Architecture Overview

## Introduction

This document provides an abstract overview of the Police System architecture, which is built using Event Sourcing and CQRS (Command Query Responsibility Segregation) patterns. The system is designed to handle high-throughput, scalable operations with strong consistency guarantees and the ability to replay and audit all system events.

## Core Architectural Patterns

### Event Sourcing

Event Sourcing is a pattern where changes to application state are stored as a sequence of events. Instead of storing the current state, the system stores all events that have occurred, and the current state can be reconstructed by replaying these events.

**Key Benefits:**
- Complete audit trail of all changes
- Ability to reconstruct state at any point in time
- Natural support for temporal queries
- Decoupling of write and read models

### CQRS (Command Query Responsibility Segregation)

CQRS separates the read and write operations of a data store. Commands (writes) and queries (reads) use different models and potentially different data stores.

**Key Benefits:**
- Independent scaling of read and write operations
- Optimized data models for specific use cases
- Reduced complexity in domain models
- Better performance through specialized read models

## System Components

The architecture consists of the following main components:

1. **Edge Servers**: Handle incoming requests, process commands, and serve queries
2. **Event Bus (Kafka)**: Central message broker for all events
3. **CQRS Projections**: Build and maintain read models from events

## Technology Stack

- **Language**: Java 17
- **Framework**: Spring Framework
- **Event Bus**: Apache Kafka
- **Stream Processing**: Kafka Streams (for projections)

## Document Structure

- [Architecture Overview](overview.md) (this document)
- [Event Sourcing](event-sourcing.md)
- [CQRS Design](cqrs-design.md)
- [Component Architecture](components.md)
- [Event Bus and Messaging](event-bus.md)
- [Data Flow](data-flow.md)

