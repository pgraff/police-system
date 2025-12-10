# ClearIncidentRequested

## Description

This event represents a request to clear an incident. It is published to Kafka when an incident clear is requested via the REST API. This is a request/command event, not a state change event.

## UML Class Diagram

```mermaid
classDiagram
    class ClearIncidentRequested {
        +String eventId
        +DateTime timestamp
        +String aggregateId
        +String incidentId
        +Instant clearedTime
    }
```

## Domain Model Effect

This event represents a **request** to clear an incident. The actual clear processing and state management happens in downstream services that consume this event.

- **Request Type**: Clear request for an incident
- **Entity Identifier**: The `incidentId` identifies the incident to clear (also used as `aggregateId`)
- **Requested Attributes**: The `clearedTime` is included in the request
- **Timestamps**: The `clearedTime` is provided as an Instant
- **State Transition**: The event represents a request to transition the incident to a cleared state
