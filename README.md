# Police Incident Management System

An event-driven police incident management system built with Java, Spring Boot, and Apache Kafka. The system follows Event Sourcing and CQRS patterns to provide a scalable, maintainable solution for managing police operations, incidents, calls, and resources.

## Overview

The Police Incident Management System is designed to handle the complete lifecycle of police operations, from receiving calls and reporting incidents to managing officers, vehicles, units, and their assignments. The system uses an event-driven architecture where all operations produce events to Kafka, ensuring a complete audit trail and enabling eventual consistency across distributed components.

### Key Features

- **Event-Driven Architecture**: All operations produce events to Kafka, representing requests/commands from the edge layer
- **RESTful API**: Comprehensive REST API for all system operations
- **Event Sourcing**: Complete audit trail through immutable events
- **CQRS Pattern**: Separation of command and query responsibilities
- **Scalable Design**: Built for horizontal scaling and high availability

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose (for Kafka)

### Running the System

1. **Start Kafka** (using Docker Compose):
   ```bash
   docker-compose up -d
   ```

2. **Build the project**:
   ```bash
   mvn clean install
   ```

3. **Run the edge server**:
   ```bash
   mvn -pl edge spring-boot:run
   ```

4. **Access the API**:
   - Base URL: `http://localhost:8080/api/v1`
   - Health check: `http://localhost:8080/api/v1/health`

## Documentation

### 📚 [API Documentation](doc/api/README.md)

Complete REST API specification and usage guide:
- [OpenAPI Specification](doc/api/openapi.yaml) - Full API specification in OpenAPI 3.0.3 format
- API endpoints organized by domain (Officers, Vehicles, Incidents, Calls, etc.)
- Request/response examples
- Event production details

### 🏗️ [Architecture Documentation](doc/architecture/README.md)

System architecture and design patterns:
- [Overview](doc/architecture/overview.md) - High-level architecture introduction
- [Event Sourcing](doc/architecture/event-sourcing.md) - Event sourcing pattern and implementation
- [CQRS Design](doc/architecture/cqrs-design.md) - Command Query Responsibility Segregation
- [Component Architecture](doc/architecture/components.md) - System components and their interactions
- [Event Bus](doc/architecture/event-bus.md) - Kafka as the event bus
- [Data Flow](doc/architecture/data-flow.md) - How data flows through the system

### 📋 [Event Documentation](doc/events/README.md)

Complete event specifications:
- [Event Index](doc/events/EVENT_INDEX.md) - Complete list of all 55 events
- Individual event specifications with UML diagrams
- Event naming conventions
- Domain model effects

All events follow the "Requested" naming convention, representing requests/commands from the edge layer:
- `RegisterOfficerRequested` - Request to register an officer
- `ReportIncidentRequested` - Request to report an incident
- `DispatchIncidentRequested` - Request to dispatch units
- And 52 more events...

### 🎯 [Domain Model](doc/domainmodel/police-incident-system.md)

Domain model documentation describing the core entities and their relationships:
- Party/Place/Thing (PPT) entities: PoliceOfficer, PoliceVehicle, Unit, Person, Location
- Moment-Interval (MI) entities: Incident, CallForService, Activity, Assignment, Shift, Dispatch
- Role entities: ResourceAssignment, InvolvedParty, OfficerShift, IncidentLocation, CallLocation

### 🔧 [Development Process](AGENTS.md)

Development guidelines and process:
- Event-driven architecture approach
- Event naming conventions
- Testing strategy
- Increment development steps (8-step process)
- Quality standards

### 📝 [Development Plan](DEVELOPMENT_PLAN.md)

Comprehensive development plan tracking all increments and their status:
- Phase-by-phase implementation plan
- Increment details with requirements, tests, and implementation
- Status tracking for completed and pending work

## System Architecture

### Event-Driven Edge Layer

The system follows an event-driven architecture where:
- **Edge servers** receive HTTP requests (commands) and produce events to Kafka
- **Events represent requests/commands** from the edge, not state changes
- **No state reconstruction** in the edge layer - events are simply produced to Kafka
- **All operations are asynchronous** via Kafka events

### Event Naming Convention

All events follow the "Requested" naming pattern:
- Registration/Creation: `Register{Entity}Requested`, `Create{Entity}Requested`, `Start{Entity}Requested`
- Updates: `Update{Entity}Requested`
- Status Changes: `Change{Entity}StatusRequested`
- Lifecycle: `Complete{Entity}Requested`, `End{Entity}Requested`, `Clear{Entity}Requested`
- Relationships: `Link{Entity}To{Target}Requested`, `Unlink{Entity}From{Target}Requested`

### Technology Stack

- **Language**: Java 17
- **Framework**: Spring Boot, Spring Framework
- **Event Bus**: Apache Kafka
- **Build Tool**: Maven
- **Testing**: JUnit 5, Kafka Test Containers

## Project Structure

```
policesystem/
├── common/              # Shared code (events, base classes)
├── edge/                # Edge server (REST API, command handlers)
├── doc/                 # Documentation
│   ├── api/            # API documentation
│   ├── architecture/   # Architecture documentation
│   ├── domainmodel/   # Domain model documentation
│   └── events/        # Event specifications
├── AGENTS.md           # Development process guidelines
├── DEVELOPMENT_PLAN.md # Development plan and tracking
└── README.md           # This file
```

## API Endpoints

The system provides REST API endpoints organized by domain:

### Core Entities
- **Officers**: Register, update, change status
- **Vehicles**: Register, update, change status
- **Units**: Create, update, change status
- **Persons**: Register, update
- **Locations**: Create, update, link/unlink to incidents and calls

### Operations
- **Incidents**: Report, update, dispatch, arrive, clear, change status
- **Calls**: Receive, update, dispatch, arrive, clear, change status, link to incidents/dispatches
- **Activities**: Start, update, complete, change status, link to incidents
- **Assignments**: Create, complete, change status, link to dispatches, manage resources
- **Shifts**: Start, end, change status, record changes, manage officer check-ins/check-outs
- **Dispatches**: Create, change status
- **Involved Parties**: Involve, update, end involvement
- **Resource Assignments**: Assign, unassign, change status

See the [API Documentation](doc/api/README.md) for complete endpoint details.

## Events

The system produces 55 different events to Kafka, organized by domain:

- **Officer Events**: RegisterOfficerRequested, UpdateOfficerRequested, ChangeOfficerStatusRequested
- **Vehicle Events**: RegisterVehicleRequested, UpdateVehicleRequested, ChangeVehicleStatusRequested
- **Unit Events**: CreateUnitRequested, UpdateUnitRequested, ChangeUnitStatusRequested
- **Person Events**: RegisterPersonRequested, UpdatePersonRequested
- **Location Events**: CreateLocationRequested, UpdateLocationRequested, Link/Unlink events
- **Incident Events**: ReportIncidentRequested, DispatchIncidentRequested, ArriveAtIncidentRequested, ClearIncidentRequested, etc.
- **Call Events**: ReceiveCallRequested, DispatchCallRequested, ArriveAtCallRequested, ClearCallRequested, etc.
- **Activity Events**: StartActivityRequested, CompleteActivityRequested, ChangeActivityStatusRequested, etc.
- **Assignment Events**: CreateAssignmentRequested, CompleteAssignmentRequested, ChangeAssignmentStatusRequested, etc.
- **Shift Events**: StartShiftRequested, EndShiftRequested, ChangeShiftStatusRequested, RecordShiftChangeRequested
- **Officer Shift Events**: CheckInOfficerRequested, CheckOutOfficerRequested, UpdateOfficerShiftRequested
- **Dispatch Events**: CreateDispatchRequested, ChangeDispatchStatusRequested
- **Resource Assignment Events**: AssignResourceRequested, UnassignResourceRequested, ChangeResourceAssignmentStatusRequested
- **Involved Party Events**: InvolvePartyRequested, EndPartyInvolvementRequested, UpdatePartyInvolvementRequested

See the [Event Index](doc/events/EVENT_INDEX.md) for the complete list.

## Kafka Topics

Events are published to domain-specific Kafka topics:
- `officer-events`
- `vehicle-events`
- `unit-events`
- `person-events`
- `location-events`
- `incident-events`
- `call-events`
- `activity-events`
- `assignment-events`
- `shift-events`
- `officer-shift-events`
- `dispatch-events`
- `resource-assignment-events`
- `involved-party-events`

## Development

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for a specific module
mvn -pl edge test

# Run a specific test class
mvn -pl edge -Dtest=OfficerControllerTest test
```

### Code Style

The project uses Checkstyle for code quality. Configuration is in `checkstyle.xml`.

### Development Process

Follow the 8-step increment development process defined in [AGENTS.md](AGENTS.md):
1. Write Requirements
2. Write Tests
3. Write Implementation Code
4. Run Tests for the Feature
5. Run All Tests (Regression Check)
6. Update Development Plan
7. Commit and Create Pull Request
8. Write Technical Demo Suggestion

## Contributing

When contributing to this project:
1. Follow the development process in [AGENTS.md](AGENTS.md)
2. Ensure all tests pass
3. Update relevant documentation
4. Follow the event naming conventions
5. Write tests before implementation (TDD)

## License

Proprietary - All rights reserved

## Support

For questions or issues, please refer to the documentation or contact the development team.
