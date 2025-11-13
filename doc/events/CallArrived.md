# CallArrived

## Description

This event is raised when units arrive at the call location.

## UML Class Diagram

```mermaid
classDiagram
    class CallArrived {
        +String eventId
        +DateTime timestamp
        +String callId
        +DateTime arrivedTime
    }
```

## Domain Model Effect

- **Modifies**: The existing `CallForService` entity identified by `callId`
- **Timestamp Update**: The `arrivedTime` attribute of the CallForService is set to the provided `arrivedTime` (typically the event timestamp)
- **Status Transition**: The call status typically transitions to "On Scene" or "In Progress"

