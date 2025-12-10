# Event Specifications

This directory contains event specifications for the police incident management system. Each event is documented in its own markdown file.

## Event Categories

Events are organized by the domain model archetypes:

- **Party/Place/Thing (PPT) Events**: Events related to physical entities (PoliceOfficer, PoliceVehicle, Unit, Person, Location)
- **Moment-Interval (MI) Events**: Events related to processes and time-based entities (Incident, CallForService, Activity, Assignment, Shift, ShiftChange, Dispatch)
- **Role Events**: Events related to role entities that connect PPTs to MIs (ResourceAssignment, InvolvedParty, OfficerShift, IncidentLocation, CallLocation)

## Event Documentation Structure

Each event specification follows this structure:

1. **Event Name and Description**: What the event represents
2. **UML Class Diagram**: Mermaid diagram showing the event structure with typed attributes, multiplicities, and composition relationships
3. **Domain Model Effect**: Description of how the event affects the domain model (entities created/modified, relationships established, state transitions)

## Event Naming Convention

**Important**: All events in this system follow the "Requested" naming convention. Events represent **requests/commands** from the edge layer, not state changes. These events are published to Kafka when operations are requested via the REST API.

Events follow a consistent naming pattern with the "Requested" suffix:
- Registration/Creation events: `Register{Entity}Requested`, `Create{Entity}Requested`, `Report{Entity}Requested`, `Receive{Entity}Requested`, `Start{Entity}Requested`
- Update events: `Update{Entity}Requested`
- Status change events: `Change{Entity}StatusRequested`
- Lifecycle events: `Complete{Entity}Requested`, `End{Entity}Requested`, `Clear{Entity}Requested`
- Relationship events: `Link{Entity}To{Target}Requested`, `Unlink{Entity}From{Target}Requested`
- Action events: `Dispatch{Entity}Requested`, `ArriveAt{Entity}Requested`, `CheckIn{Entity}Requested`, `CheckOut{Entity}Requested`

## Event-Driven Architecture

This system follows an event-driven architecture where:
- **Edge servers** receive HTTP requests (commands) and produce events to Kafka
- **Events represent requests/commands** from the edge, not state changes
- **No state reconstruction** in the edge layer - events are simply produced to Kafka
- Events are published asynchronously and processed by downstream services

## Examples

- `RegisterOfficerRequested` - Request to register a new officer (not `OfficerRegistered`)
- `StartActivityRequested` - Request to start an activity (not `ActivityStarted`)
- `CheckInOfficerRequested` - Request to check in an officer to a shift (not `OfficerCheckedIn`)
- `LinkCallToIncidentRequested` - Request to link a call to an incident (not `CallLinkedToIncident`)
