# UpdateUnitRequested

## Description

This event represents a request to update an existing unit's information. It is published to Kafka when a unit update is requested via the REST API. This is a request/command event, not a state change event. All fields are nullable to support partial updates - null fields mean "don't update this field".

## UML Class Diagram

```mermaid
classDiagram
    class UpdateUnitRequested {
        +String eventId
        +DateTime timestamp
        +String aggregateId
        +String unitId
        +String unitType
        +String status
    }
```

## Domain Model Effect

This event represents a **request** to update an existing `Unit` entity. The actual update and state management happens in downstream services that consume this event.

- **Request Type**: Update request for an existing unit
- **Entity Identifier**: The `unitId` identifies the unit to update (also used as `aggregateId`)
- **Partial Updates**: All fields are nullable - only non-null fields will be updated
- **Updated Attributes**: Any provided attributes (unitType, status) are included in the update request
- **Note**: The `unitId` cannot be changed as it serves as the entity identifier
- **Note**: Status changes should use the `ChangeUnitStatusRequested` event instead
