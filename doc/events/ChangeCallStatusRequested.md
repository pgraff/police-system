# ChangeCallStatusRequested

## Description

This event represents a request to change a call's status. It is published to Kafka when a call status change is requested via the REST API. This is a request/command event, not a state change event.

## UML Class Diagram

```mermaid
classDiagram
    class ChangeCallStatusRequested {
        +String eventId
        +DateTime timestamp
        +String aggregateId
        +String callId
        +String status
    }
```

## Domain Model Effect

This event represents a **request** to change the status of an existing `CallForService` entity. The actual status change and state management happens in downstream services that consume this event.

- **Request Type**: Status change request for an existing call
- **Entity Identifier**: The `callId` identifies the call whose status should be changed (also used as `aggregateId`)
- **Status Update**: The `status` attribute contains the requested new status value
- **Valid Status Values**: Received, Dispatched, Arrived, In-Progress, Cleared, Closed
- **State Transition**: The event represents a request for a state transition, which will be processed by downstream services
