# UpdateIncidentRequested

## Description

This event represents a request to update an existing incident's information. It is published to Kafka when an incident update is requested via the REST API. This is a request/command event, not a state change event. All fields are nullable to support partial updates - null fields mean "don't update this field".

## UML Class Diagram

```mermaid
classDiagram
    class UpdateIncidentRequested {
        +String eventId
        +DateTime timestamp
        +String aggregateId
        +String incidentId
        +String priority
        +String description
        +String incidentType
    }
```

## Domain Model Effect

This event represents a **request** to update an existing `Incident` entity. The actual update and state management happens in downstream services that consume this event.

- **Request Type**: Update request for an existing incident
- **Entity Identifier**: The `incidentId` identifies the incident to update (also used as `aggregateId`)
- **Partial Updates**: All fields are nullable - only non-null fields will be updated
- **Updated Attributes**: Any provided attributes (priority, description, incidentType) are included in the update request
- **Note**: The `incidentId` cannot be changed as it serves as the entity identifier
- **Note**: Status changes should use the `ChangeIncidentStatusRequested` event instead
- **Enum Values**: The `priority` and `incidentType` are provided as string enum names if provided
