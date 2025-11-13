# CallStatusChanged

## Description

This event is raised when a call for service's status changes.

## UML Class Diagram

```mermaid
classDiagram
    class CallStatusChanged {
        +String eventId
        +DateTime timestamp
        +String callId
        +String previousStatus
        +String newStatus
    }
```

## Domain Model Effect

- **Modifies**: The existing `CallForService` entity identified by `callId`
- **Status Update**: The `status` attribute of the CallForService is updated from `previousStatus` to `newStatus`
- **State Transition**: The event documents the state transition for audit purposes

