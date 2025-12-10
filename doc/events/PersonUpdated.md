# UpdatePersonRequested

## Description

This event represents a request to update an existing person's information. It is published to Kafka when a person update is requested via the REST API. This is a request/command event, not a state change event. All fields are nullable to support partial updates - null fields mean "don't update this field".

## UML Class Diagram

```mermaid
classDiagram
    class UpdatePersonRequested {
        +String eventId
        +DateTime timestamp
        +String aggregateId
        +String personId
        +String firstName
        +String lastName
        +String dateOfBirth
        +String gender
        +String race
        +String phoneNumber
    }
```

## Domain Model Effect

This event represents a **request** to update an existing `Person` entity. The actual update and state management happens in downstream services that consume this event.

- **Request Type**: Update request for an existing person
- **Entity Identifier**: The `personId` identifies the person to update (also used as `aggregateId`)
- **Partial Updates**: All fields are nullable - only non-null fields will be updated
- **Updated Attributes**: Any provided attributes (firstName, lastName, dateOfBirth, gender, race, phoneNumber) are included in the update request
- **Note**: The `personId` cannot be changed as it serves as the entity identifier
- **Date Format**: The `dateOfBirth` is provided as a string in ISO-8601 format (yyyy-MM-dd) if provided
- **Enum Values**: The `gender` and `race` are provided as string enum names if provided
