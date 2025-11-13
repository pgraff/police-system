# ResourceAssignmentStatusChanged

## Description

This event is raised when a ResourceAssignment's status changes.

## UML Class Diagram

```mermaid
classDiagram
    class ResourceAssignmentStatusChanged {
        +String eventId
        +DateTime timestamp
        +String roleId
        +String previousStatus
        +String newStatus
    }
```

## Domain Model Effect

- **Modifies**: The existing `ResourceAssignment` role entity identified by `roleId`
- **Status Update**: The `status` attribute of the ResourceAssignment is updated from `previousStatus` to `newStatus`
- **State Transition**: The event documents the state transition for audit purposes

