# Event Index

This document provides a complete index of all events in the police incident management system, organized by domain model archetype.

## Party/Place/Thing (PPT) Events

### PoliceOfficer Events
- `OfficerRegistered` - New officer added to system
- `OfficerUpdated` - Officer information modified
- `OfficerStatusChanged` - Officer status updated

### PoliceVehicle Events
- `VehicleRegistered` - New vehicle added to system
- `VehicleUpdated` - Vehicle information modified
- `VehicleStatusChanged` - Vehicle status updated

### Unit Events
- `UnitCreated` - New unit created
- `UnitUpdated` - Unit information modified
- `UnitStatusChanged` - Unit status updated

### Person Events
- `PersonRegistered` - New person added to system
- `PersonUpdated` - Person information modified

### Location Events
- `LocationCreated` - New location added
- `LocationUpdated` - Location information modified

## Moment-Interval (MI) Events

### Incident Events
- `IncidentReported` - New incident created
- `IncidentDispatched` - Incident dispatched to units
- `IncidentArrived` - Units arrived at incident
- `IncidentCleared` - Incident cleared/completed
- `IncidentStatusChanged` - Incident status updated
- `IncidentUpdated` - Incident information modified

### CallForService Events
- `CallReceived` - New call for service received
- `CallDispatched` - Call dispatched to units
- `CallArrived` - Units arrived at call location
- `CallCleared` - Call cleared/completed
- `CallStatusChanged` - Call status updated
- `CallUpdated` - Call information modified
- `CallLinkedToIncident` - Call linked to incident
- `CallLinkedToDispatch` - Call linked to dispatch

### Activity Events
- `ActivityStarted` - Activity initiated
- `ActivityCompleted` - Activity finished
- `ActivityStatusChanged` - Activity status updated
- `ActivityUpdated` - Activity information modified
- `ActivityLinkedToIncident` - Activity linked to incident

### Assignment Events
- `AssignmentCreated` - Resource assigned to incident/call
- `AssignmentCompleted` - Assignment finished
- `AssignmentStatusChanged` - Assignment status updated
- `AssignmentLinkedToDispatch` - Assignment linked to dispatch

### Shift Events
- `ShiftStarted` - Shift begins
- `ShiftEnded` - Shift ends
- `ShiftStatusChanged` - Shift status updated

### ShiftChange Events
- `ShiftChangeRecorded` - Shift change documented

### Dispatch Events
- `DispatchCreated` - New dispatch created
- `DispatchStatusChanged` - Dispatch status updated

## Role Events

### ResourceAssignment Events
- `ResourceAssigned` - Resource assigned to assignment
- `ResourceUnassigned` - Resource removed from assignment
- `ResourceAssignmentStatusChanged` - Resource assignment status updated

### InvolvedParty Events
- `PartyInvolved` - Person involved in incident/call/activity
- `PartyInvolvementEnded` - Person no longer involved
- `PartyInvolvementUpdated` - Involvement details modified

### OfficerShift Events
- `OfficerCheckedIn` - Officer checks in to shift
- `OfficerCheckedOut` - Officer checks out of shift
- `OfficerShiftUpdated` - Officer shift details modified

### IncidentLocation Events
- `LocationLinkedToIncident` - Location linked to incident
- `LocationUnlinkedFromIncident` - Location removed from incident

### CallLocation Events
- `LocationLinkedToCall` - Location linked to call
- `LocationUnlinkedFromCall` - Location removed from call

## Total Event Count

**50 events** covering all entity lifecycle transitions and relationship changes in the domain model.

