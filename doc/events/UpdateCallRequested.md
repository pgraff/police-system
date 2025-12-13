# UpdateCallRequested

## Description

This event represents a request to update an existing call's information. It is published to Kafka when a call update is requested via the REST API. This is a request/command event, not a state change event. All fields are nullable to support partial updates - null fields mean "don't update this field".

## UML Class Diagram

```mermaid
classDiagram
    class UpdateCallRequested {
        +String eventId
        +DateTime timestamp
        +String aggregateId
        +String callId
        +String priority
        +String description
        +String callType
    }
```

## Domain Model Effect

This event represents a **request** to update an existing `CallForService` entity. The actual update and state management happens in downstream services that consume this event.

- **Request Type**: Update request for an existing call
- **Entity Identifier**: The `callId` identifies the call to update (also used as `aggregateId`)
- **Partial Updates**: All fields are nullable - only non-null fields will be updated
- **Updated Attributes**: Any provided attributes (priority, description, callType) are included in the update request
- **Note**: The `callId` cannot be changed as it serves as the entity identifier
- **Note**: Status changes should use the `ChangeCallStatusRequested` event instead
- **Enum Values**: The `priority` and `callType` are provided as string enum names if provided
