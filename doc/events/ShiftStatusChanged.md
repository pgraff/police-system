# ShiftStatusChanged

## Description

This event is raised when a shift's status changes.

## UML Class Diagram

```mermaid
classDiagram
    class ShiftStatusChanged {
        +String eventId
        +DateTime timestamp
        +String shiftId
        +String previousStatus
        +String newStatus
    }
```

## Domain Model Effect

- **Modifies**: The existing `Shift` entity identified by `shiftId`
- **Status Update**: The `status` attribute of the Shift is updated from `previousStatus` to `newStatus`
- **State Transition**: The event documents the state transition for audit purposes

