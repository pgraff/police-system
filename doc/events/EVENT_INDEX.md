# Event Index

This document provides a complete index of all events in the police incident management system, organized by domain model archetype.

**Note**: All events follow the "Requested" naming convention, representing requests/commands from the edge layer, not state changes. These events are published to Kafka when operations are requested via the REST API.

## Party/Place/Thing (PPT) Events

### PoliceOfficer Events
- [`RegisterOfficerRequested`](RegisterOfficerRequested.md) - Request to register a new officer
- [`UpdateOfficerRequested`](UpdateOfficerRequested.md) - Request to update officer information
- [`ChangeOfficerStatusRequested`](ChangeOfficerStatusRequested.md) - Request to change officer status

### PoliceVehicle Events
- [`RegisterVehicleRequested`](RegisterVehicleRequested.md) - Request to register a new vehicle
- [`UpdateVehicleRequested`](UpdateVehicleRequested.md) - Request to update vehicle information
- [`ChangeVehicleStatusRequested`](ChangeVehicleStatusRequested.md) - Request to change vehicle status

### Unit Events
- [`CreateUnitRequested`](CreateUnitRequested.md) - Request to create a new unit
- [`UpdateUnitRequested`](UpdateUnitRequested.md) - Request to update unit information
- [`ChangeUnitStatusRequested`](ChangeUnitStatusRequested.md) - Request to change unit status

### Person Events
- [`RegisterPersonRequested`](RegisterPersonRequested.md) - Request to register a new person
- [`UpdatePersonRequested`](UpdatePersonRequested.md) - Request to update person information

### Location Events
- [`CreateLocationRequested`](CreateLocationRequested.md) - Request to create a new location
- [`UpdateLocationRequested`](UpdateLocationRequested.md) - Request to update location information
- [`LinkLocationToIncidentRequested`](LinkLocationToIncidentRequested.md) - Request to link location to incident
- [`UnlinkLocationFromIncidentRequested`](UnlinkLocationFromIncidentRequested.md) - Request to unlink location from incident
- [`LinkLocationToCallRequested`](LinkLocationToCallRequested.md) - Request to link location to call
- [`UnlinkLocationFromCallRequested`](UnlinkLocationFromCallRequested.md) - Request to unlink location from call

## Moment-Interval (MI) Events

### Incident Events
- [`ReportIncidentRequested`](ReportIncidentRequested.md) - Request to report a new incident
- [`DispatchIncidentRequested`](DispatchIncidentRequested.md) - Request to dispatch incident to units
- [`ArriveAtIncidentRequested`](ArriveAtIncidentRequested.md) - Request to record arrival at incident
- [`ClearIncidentRequested`](ClearIncidentRequested.md) - Request to clear incident
- [`ChangeIncidentStatusRequested`](ChangeIncidentStatusRequested.md) - Request to change incident status
- [`UpdateIncidentRequested`](UpdateIncidentRequested.md) - Request to update incident information

### CallForService Events
- [`ReceiveCallRequested`](ReceiveCallRequested.md) - Request to receive a new call for service
- [`DispatchCallRequested`](DispatchCallRequested.md) - Request to dispatch call to units
- [`ArriveAtCallRequested`](ArriveAtCallRequested.md) - Request to record arrival at call location
- [`ClearCallRequested`](ClearCallRequested.md) - Request to clear call
- [`ChangeCallStatusRequested`](ChangeCallStatusRequested.md) - Request to change call status
- [`UpdateCallRequested`](UpdateCallRequested.md) - Request to update call information
- [`LinkCallToIncidentRequested`](LinkCallToIncidentRequested.md) - Request to link call to incident
- [`LinkCallToDispatchRequested`](LinkCallToDispatchRequested.md) - Request to link call to dispatch

### Activity Events
- [`StartActivityRequested`](StartActivityRequested.md) - Request to start an activity
- [`CompleteActivityRequested`](CompleteActivityRequested.md) - Request to complete an activity
- [`ChangeActivityStatusRequested`](ChangeActivityStatusRequested.md) - Request to change activity status
- [`UpdateActivityRequested`](UpdateActivityRequested.md) - Request to update activity information
- [`LinkActivityToIncidentRequested`](LinkActivityToIncidentRequested.md) - Request to link activity to incident

### Assignment Events
- [`CreateAssignmentRequested`](CreateAssignmentRequested.md) - Request to create an assignment
- [`CompleteAssignmentRequested`](CompleteAssignmentRequested.md) - Request to complete an assignment
- [`ChangeAssignmentStatusRequested`](ChangeAssignmentStatusRequested.md) - Request to change assignment status
- [`LinkAssignmentToDispatchRequested`](LinkAssignmentToDispatchRequested.md) - Request to link assignment to dispatch

### Shift Events
- [`StartShiftRequested`](StartShiftRequested.md) - Request to start a shift
- [`EndShiftRequested`](EndShiftRequested.md) - Request to end a shift
- [`ChangeShiftStatusRequested`](ChangeShiftStatusRequested.md) - Request to change shift status
- [`RecordShiftChangeRequested`](RecordShiftChangeRequested.md) - Request to record a shift change

### Dispatch Events
- [`CreateDispatchRequested`](CreateDispatchRequested.md) - Request to create a new dispatch
- [`ChangeDispatchStatusRequested`](ChangeDispatchStatusRequested.md) - Request to change dispatch status

## Role Events

### ResourceAssignment Events
- [`AssignResourceRequested`](AssignResourceRequested.md) - Request to assign a resource to an assignment
- [`UnassignResourceRequested`](UnassignResourceRequested.md) - Request to unassign a resource from an assignment
- [`ChangeResourceAssignmentStatusRequested`](ChangeResourceAssignmentStatusRequested.md) - Request to change resource assignment status

### InvolvedParty Events
- [`InvolvePartyRequested`](InvolvePartyRequested.md) - Request to involve a party in incident/call/activity
- [`EndPartyInvolvementRequested`](EndPartyInvolvementRequested.md) - Request to end party involvement
- [`UpdatePartyInvolvementRequested`](UpdatePartyInvolvementRequested.md) - Request to update party involvement information

### OfficerShift Events
- [`CheckInOfficerRequested`](CheckInOfficerRequested.md) - Request to check in an officer to a shift
- [`CheckOutOfficerRequested`](CheckOutOfficerRequested.md) - Request to check out an officer from a shift
- [`UpdateOfficerShiftRequested`](UpdateOfficerShiftRequested.md) - Request to update officer shift information

## Total Event Count

**55 events** covering all entity lifecycle transitions and relationship changes in the domain model. All events represent requests/commands from the edge layer and are published to Kafka.
