# Event Index

This document provides a complete index of all events in the police incident management system, organized by domain model archetype.

## Party/Place/Thing (PPT) Events

### PoliceOfficer Events
- [`OfficerRegistered`](OfficerRegistered.md) - New officer added to system
- [`OfficerUpdated`](OfficerUpdated.md) - Officer information modified
- [`OfficerStatusChanged`](OfficerStatusChanged.md) - Officer status updated

### PoliceVehicle Events
- [`VehicleRegistered`](VehicleRegistered.md) - New vehicle added to system
- [`VehicleUpdated`](VehicleUpdated.md) - Vehicle information modified
- [`VehicleStatusChanged`](VehicleStatusChanged.md) - Vehicle status updated

### Unit Events
- [`UnitCreated`](UnitCreated.md) - New unit created
- [`UnitUpdated`](UnitUpdated.md) - Unit information modified
- [`UnitStatusChanged`](UnitStatusChanged.md) - Unit status updated

### Person Events
- [`PersonRegistered`](PersonRegistered.md) - New person added to system
- [`PersonUpdated`](PersonUpdated.md) - Person information modified

### Location Events
- [`LocationCreated`](LocationCreated.md) - New location added
- [`LocationUpdated`](LocationUpdated.md) - Location information modified

## Moment-Interval (MI) Events

### Incident Events
- [`IncidentReported`](IncidentReported.md) - New incident created
- [`IncidentDispatched`](IncidentDispatched.md) - Incident dispatched to units
- [`IncidentArrived`](IncidentArrived.md) - Units arrived at incident
- [`IncidentCleared`](IncidentCleared.md) - Incident cleared/completed
- [`IncidentStatusChanged`](IncidentStatusChanged.md) - Incident status updated
- [`IncidentUpdated`](IncidentUpdated.md) - Incident information modified

### CallForService Events
- [`CallReceived`](CallReceived.md) - New call for service received
- [`CallDispatched`](CallDispatched.md) - Call dispatched to units
- [`CallArrived`](CallArrived.md) - Units arrived at call location
- [`CallCleared`](CallCleared.md) - Call cleared/completed
- [`CallStatusChanged`](CallStatusChanged.md) - Call status updated
- [`CallUpdated`](CallUpdated.md) - Call information modified
- [`CallLinkedToIncident`](CallLinkedToIncident.md) - Call linked to incident
- [`CallLinkedToDispatch`](CallLinkedToDispatch.md) - Call linked to dispatch

### Activity Events
- [`ActivityStarted`](ActivityStarted.md) - Activity initiated
- [`ActivityCompleted`](ActivityCompleted.md) - Activity finished
- [`ActivityStatusChanged`](ActivityStatusChanged.md) - Activity status updated
- [`ActivityUpdated`](ActivityUpdated.md) - Activity information modified
- [`ActivityLinkedToIncident`](ActivityLinkedToIncident.md) - Activity linked to incident

### Assignment Events
- [`AssignmentCreated`](AssignmentCreated.md) - Resource assigned to incident/call
- [`AssignmentCompleted`](AssignmentCompleted.md) - Assignment finished
- [`AssignmentStatusChanged`](AssignmentStatusChanged.md) - Assignment status updated
- [`AssignmentLinkedToDispatch`](AssignmentLinkedToDispatch.md) - Assignment linked to dispatch

### Shift Events
- [`ShiftStarted`](ShiftStarted.md) - Shift begins
- [`ShiftEnded`](ShiftEnded.md) - Shift ends
- [`ShiftStatusChanged`](ShiftStatusChanged.md) - Shift status updated

### ShiftChange Events
- [`ShiftChangeRecorded`](ShiftChangeRecorded.md) - Shift change documented

### Dispatch Events
- [`DispatchCreated`](DispatchCreated.md) - New dispatch created
- [`DispatchStatusChanged`](DispatchStatusChanged.md) - Dispatch status updated

## Role Events

### ResourceAssignment Events
- [`ResourceAssigned`](ResourceAssigned.md) - Resource assigned to assignment
- [`ResourceUnassigned`](ResourceUnassigned.md) - Resource removed from assignment
- [`ResourceAssignmentStatusChanged`](ResourceAssignmentStatusChanged.md) - Resource assignment status updated

### InvolvedParty Events
- [`PartyInvolved`](PartyInvolved.md) - Person involved in incident/call/activity
- [`PartyInvolvementEnded`](PartyInvolvementEnded.md) - Person no longer involved
- [`PartyInvolvementUpdated`](PartyInvolvementUpdated.md) - Involvement details modified

### OfficerShift Events
- [`OfficerCheckedIn`](OfficerCheckedIn.md) - Officer checks in to shift
- [`OfficerCheckedOut`](OfficerCheckedOut.md) - Officer checks out of shift
- [`OfficerShiftUpdated`](OfficerShiftUpdated.md) - Officer shift details modified

### IncidentLocation Events
- [`LocationLinkedToIncident`](LocationLinkedToIncident.md) - Location linked to incident
- [`LocationUnlinkedFromIncident`](LocationUnlinkedFromIncident.md) - Location removed from incident

### CallLocation Events
- [`LocationLinkedToCall`](LocationLinkedToCall.md) - Location linked to call
- [`LocationUnlinkedFromCall`](LocationUnlinkedFromCall.md) - Location removed from call

## Total Event Count

**50 events** covering all entity lifecycle transitions and relationship changes in the domain model.

