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

Events follow a consistent naming pattern:
- Creation events: `{Entity}Registered`, `{Entity}Created`, `{Entity}Reported`, `{Entity}Received`, `{Entity}Started`
- Update events: `{Entity}Updated`
- Status change events: `{Entity}StatusChanged`
- Lifecycle events: `{Entity}Completed`, `{Entity}Ended`, `{Entity}Cleared`
- Relationship events: `{Action}To{Entity}`, `{Entity}Linked`, `{Entity}Unlinked`

