# CreateUnitRequested

## Description

This event represents a request to create a new unit. It is published to Kafka when a unit creation is requested via the REST API. This is a request/command event, not a state change event.

## UML Class Diagram

```mermaid
classDiagram
    class CreateUnitRequested {
        +String eventId
        +DateTime timestamp
        +String aggregateId
        +String unitId
        +String unitType
        +String status
    }
```

## Domain Model Effect

This event represents a **request** to create a new `Unit` entity. The actual creation and state management happens in downstream services that consume this event.

- **Request Type**: Creation request for a new unit
- **Entity Identifier**: The `unitId` serves as the unique identifier (also used as `aggregateId`)
- **Requested Attributes**: All provided attributes (unitType, status) are included in the request
- **Status**: The `status` attribute is provided in the request (typically "Available")
