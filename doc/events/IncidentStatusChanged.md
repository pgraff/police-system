# IncidentStatusChanged

## Description

This event is raised when an incident's status changes.

## UML Class Diagram

```mermaid
classDiagram
    class IncidentStatusChanged {
        +String eventId
        +DateTime timestamp
        +String incidentId
        +String previousStatus
        +String newStatus
    }
```

## Domain Model Effect

- **Modifies**: The existing `Incident` entity identified by `incidentId`
- **Status Update**: The `status` attribute of the Incident is updated from `previousStatus` to `newStatus`
- **State Transition**: The event documents the state transition for audit purposes

