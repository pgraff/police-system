# ActivityStatusChanged

## Description

This event is raised when an activity's status changes.

## UML Class Diagram

```mermaid
classDiagram
    class ActivityStatusChanged {
        +String eventId
        +DateTime timestamp
        +String activityId
        +String previousStatus
        +String newStatus
    }
```

## Domain Model Effect

- **Modifies**: The existing `Activity` entity identified by `activityId`
- **Status Update**: The `status` attribute of the Activity is updated from `previousStatus` to `newStatus`
- **State Transition**: The event documents the state transition for audit purposes

