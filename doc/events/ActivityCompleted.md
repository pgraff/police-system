# CompleteActivityRequested

## Description

This event represents a request to complete an activity. It is published to Kafka when an activity completion is requested via the REST API. This is a request/command event, not a state change event.

## UML Class Diagram

```mermaid
classDiagram
    class CompleteActivityRequested {
        +String eventId
        +DateTime timestamp
        +String aggregateId
        +String activityId
        +Instant completedTime
    }
```

## Domain Model Effect

This event represents a **request** to complete an activity. The actual completion processing and state management happens in downstream services that consume this event.

- **Request Type**: Completion request for an activity
- **Entity Identifier**: The `activityId` identifies the activity to complete (also used as `aggregateId`)
- **Requested Attributes**: The `completedTime` is included in the request
- **Timestamps**: The `completedTime` is provided as an Instant
- **State Transition**: The event represents a request to transition the activity to a completed state
