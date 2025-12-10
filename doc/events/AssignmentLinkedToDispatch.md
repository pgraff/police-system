# LinkAssignmentToDispatchRequested

## Description

This event represents a request to link an assignment to a dispatch. It is published to Kafka when an assignment is linked to a dispatch via the REST API. This is a request/command event, not a state change event.

## UML Class Diagram

```mermaid
classDiagram
    class LinkAssignmentToDispatchRequested {
        +String eventId
        +DateTime timestamp
        +String aggregateId
        +String assignmentId
        +String dispatchId
    }
```

## Domain Model Effect

This event represents a **request** to link an assignment to a dispatch. The actual relationship creation and state management happens in downstream services that consume this event.

- **Request Type**: Link request to associate an assignment with a dispatch
- **Aggregate Identifier**: The `assignmentId` is used as `aggregateId`
- **Requested Attributes**: Both `assignmentId` and `dispatchId` are included in the request
- **Relationship**: The event represents a request to establish a relationship between the Assignment and Dispatch entities
