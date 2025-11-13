# PersonRegistered

## Description

This event is raised when a new person is registered in the system.

## UML Class Diagram

```mermaid
classDiagram
    class PersonRegistered {
        +String eventId
        +DateTime timestamp
        +String personId
        +String firstName
        +String lastName
        +DateTime dateOfBirth
        +String gender
        +String race
        +String phoneNumber
    }
```

## Domain Model Effect

- **Creates**: A new `Person` entity with the provided attributes
- **Entity Identifier**: The `personId` serves as the unique identifier
- **Attributes**: All provided attributes (personId, firstName, lastName, dateOfBirth, gender, race, phoneNumber) are set on the new Person entity

