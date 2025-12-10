# ArriveAtIncidentRequested

## Description

This event represents a request to record arrival at an incident. It is published to Kafka when an arrival is requested via the REST API. This is a request/command event, not a state change event.

## UML Class Diagram

```mermaid
classDiagram
    class ArriveAtIncidentRequested {
        +String eventId
        +DateTime timestamp
        +String aggregateId
        +String incidentId
        +Instant arrivedTime
    }
```

## Domain Model Effect

This event represents a **request** to record arrival at an incident. The actual arrival processing and state management happens in downstream services that consume this event.

- **Request Type**: Arrival request for an incident
- **Entity Identifier**: The `incidentId` identifies the incident where arrival is recorded (also used as `aggregateId`)
- **Requested Attributes**: The `arrivedTime` is included in the request
- **Timestamps**: The `arrivedTime` is provided as an Instant
- **State Transition**: The event represents a request to transition the incident to an arrived state
