# LinkCallToDispatchRequested

## Description

This event represents a request to link a call to a dispatch. It is published to Kafka when a call is linked to a dispatch via the REST API. This is a request/command event, not a state change event.

## UML Class Diagram

```mermaid
classDiagram
    class LinkCallToDispatchRequested {
        +String eventId
        +DateTime timestamp
        +String aggregateId
        +String callId
        +String dispatchId
    }
```

## Domain Model Effect

This event represents a **request** to link a call to a dispatch. The actual relationship creation and state management happens in downstream services that consume this event.

- **Request Type**: Link request to associate a call with a dispatch
- **Aggregate Identifier**: The `callId` is used as `aggregateId`
- **Requested Attributes**: Both `callId` and `dispatchId` are included in the request
- **Relationship**: The event represents a request to establish a relationship between the CallForService and Dispatch entities
