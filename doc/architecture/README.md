# Police System Architecture Documentation

This directory contains the architecture documentation for the Police System, which is built using Event Sourcing and CQRS patterns.

## Documentation Structure

### [Overview](overview.md)
High-level introduction to the architecture, core patterns, and system components.

### [Event Sourcing](event-sourcing.md)
Detailed explanation of the Event Sourcing pattern, event lifecycle, and how events are stored and replayed.

### [CQRS Design](cqrs-design.md)
Description of the Command Query Responsibility Segregation pattern, including command and query sides, and their separation.

### [Component Architecture](components.md)
Detailed description of system components: Edge Servers, Event Bus (Kafka), and CQRS Projections.

### [Event Bus and Messaging](event-bus.md)
Comprehensive guide to Kafka and NATS/JetStream as event buses, including topics, subjects, consumption patterns, and the double-publish pattern.

### [Data Flow](data-flow.md)
Detailed description of how data flows through the system: commands, events, queries, and integration flows.

### [Docker Compose Infrastructure](docker-compose.md)
Complete guide to the infrastructure services provided by docker-compose, including Kafka cluster, NATS cluster, databases, and admin UIs.

## Architecture Principles

### Event Sourcing
- All state changes are captured as immutable events
- Current state can be reconstructed by replaying events
- Complete audit trail of all changes
- Events are stored in Kafka

### CQRS
- Commands (writes) and queries (reads) are separated
- Write model optimized for transactions
- Read model optimized for queries
- Eventual consistency between read and write models

### Technology Stack
- **Language**: Java 17
- **Framework**: Spring Framework
- **Primary Event Bus**: Apache Kafka (event sourcing and long-term storage)
- **Secondary Event Bus**: NATS/JetStream (critical events for near realtime processing)
- **Stream Processing**: Kafka Streams

## Key Components

1. **Edge Servers**: Handle commands and queries, built with Spring
2. **Event Buses**: 
   - **Kafka**: Primary event store and message broker for all events
   - **NATS/JetStream**: Secondary event bus for critical events requiring near realtime processing
3. **CQRS Projections**: Build read models using Kafka Streams and Spring

## Design Decisions

- **Abstract Architecture**: This documentation focuses on architectural concepts rather than specific implementation details
- **Technology Choices**: Java, Spring, and Kafka were chosen for their maturity and ecosystem support
- **Scalability**: Architecture is designed for horizontal scaling
- **Resilience**: System can recover from failures through event replay

## Future Documentation

As the system evolves, additional documentation may be added for:
- Specific domain models
- API specifications
- Deployment architecture
- Security architecture
- Performance tuning
- Operational procedures

