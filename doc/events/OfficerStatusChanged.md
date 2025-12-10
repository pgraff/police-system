# ChangeOfficerStatusRequested

## Description

This event represents a request to change a police officer's status. It is published to Kafka when an officer status change is requested via the REST API. This is a request/command event, not a state change event.

## UML Class Diagram

```mermaid
classDiagram
    class ChangeOfficerStatusRequested {
        +String eventId
        +DateTime timestamp
        +String aggregateId
        +String badgeNumber
        +String status
    }
```

## Domain Model Effect

This event represents a **request** to change the status of an existing `PoliceOfficer` entity. The actual status change and state management happens in downstream services that consume this event.

- **Request Type**: Status change request for an existing police officer
- **Entity Identifier**: The `badgeNumber` identifies the officer whose status should be changed (also used as `aggregateId`)
- **Status Update**: The `status` attribute contains the requested new status value
- **Valid Status Values**: Active, On-Duty, Off-Duty, Suspended, Retired
- **State Transition**: The event represents a request for a state transition, which will be processed by downstream services
