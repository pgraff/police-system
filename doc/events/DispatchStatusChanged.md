# DispatchStatusChanged

## Description

This event is raised when a dispatch's status changes.

## UML Class Diagram

```mermaid
classDiagram
    class DispatchStatusChanged {
        +String eventId
        +DateTime timestamp
        +String dispatchId
        +String previousStatus
        +String newStatus
    }
```

## Domain Model Effect

- **Modifies**: The existing `Dispatch` entity identified by `dispatchId`
- **Status Update**: The `status` attribute of the Dispatch is updated from `previousStatus` to `newStatus`
- **State Transition**: The event documents the state transition for audit purposes

