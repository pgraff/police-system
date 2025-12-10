# UnlinkLocationFromCallRequested

## Description

This event represents a request to unlink a location from a call for service. It is published to Kafka when a location is unlinked from a call via the REST API. This is a request/command event, not a state change event.

## UML Class Diagram

```mermaid
classDiagram
    class UnlinkLocationFromCallRequested {
        +String eventId
        +DateTime timestamp
        +String aggregateId
        +String callId
        +String locationId
    }
```

## Domain Model Effect

This event represents a **request** to unlink a location from a call for service. The actual relationship removal and state management happens in downstream services that consume this event.

- **Request Type**: Unlink request to remove the association between a location and a call
- **Aggregate Identifier**: The `locationId` is used as `aggregateId`
- **Requested Attributes**: Both `callId` and `locationId` are included in the request to identify the relationship to remove
- **Relationship**: The event represents a request to remove the relationship between the Location and CallForService entities
