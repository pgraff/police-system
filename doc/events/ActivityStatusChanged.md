# ChangeActivityStatusRequested

## Description

This event represents a request to change an activity's status. It is published to Kafka when an activity status change is requested via the REST API. This is a request/command event, not a state change event.

## UML Class Diagram

```mermaid
classDiagram
    class ChangeActivityStatusRequested {
        +String eventId
        +DateTime timestamp
        +String aggregateId
        +String activityId
        +String status
    }
```

## Domain Model Effect

This event represents a **request** to change the status of an existing `Activity` entity. The actual status change and state management happens in downstream services that consume this event.

- **Request Type**: Status change request for an existing activity
- **Entity Identifier**: The `activityId` identifies the activity whose status should be changed (also used as `aggregateId`)
- **Status Update**: The `status` attribute contains the requested new status value
- **Valid Status Values**: Started, In-Progress, Completed, Cancelled
- **State Transition**: The event represents a request for a state transition, which will be processed by downstream services
