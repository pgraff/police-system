# ChangeVehicleStatusRequested

## Description

This event represents a request to change a police vehicle's status. It is published to Kafka when a vehicle status change is requested via the REST API. This is a request/command event, not a state change event.

## UML Class Diagram

```mermaid
classDiagram
    class ChangeVehicleStatusRequested {
        +String eventId
        +DateTime timestamp
        +String aggregateId
        +String unitId
        +String status
    }
```

## Domain Model Effect

This event represents a **request** to change the status of an existing `PoliceVehicle` entity. The actual status change and state management happens in downstream services that consume this event.

- **Request Type**: Status change request for an existing police vehicle
- **Entity Identifier**: The `unitId` identifies the vehicle whose status should be changed (also used as `aggregateId`)
- **Status Update**: The `status` attribute contains the requested new status value
- **Valid Status Values**: Available, Assigned, In-Use, Maintenance, Out-of-Service
- **State Transition**: The event represents a request for a state transition, which will be processed by downstream services
