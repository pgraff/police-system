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
- Docker and Docker Compose (for Kafka and NATS)

### Running the System

1. **Start Infrastructure Services** (using Docker Compose):
   ```bash
   docker compose up -d
   ```
   This starts:
   - **Kafka** (3 brokers) on ports 9092-9094
   - **NATS** (3-node cluster) on ports 4222-4224
   - **PostgreSQL** on port 5432
   - **MongoDB** on port 27017
   - **InfluxDB** on port 8086
   - **Elasticsearch** on port 9200 (for event indexing and search)
   - **Kafka Connect** on port 8083 (for Elasticsearch indexing)
   - **Kafka UI** on http://localhost:8080
   - **NATS Tower** on http://localhost:8099

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
   - Swagger UI: `http://localhost:8080/swagger-ui.html`

5. **Access Admin UIs**:
   - **Kafka UI**: http://localhost:8080 (Kafka management)
   - **Kafka Connect REST API**: http://localhost:8083 (Connector management)
   - **NATS Tower**: http://localhost:8099 (NATS cluster management)
   - **NATS Monitoring**: 
     - nats-1: http://localhost:8222
     - nats-2: http://localhost:8223
     - nats-3: http://localhost:8224

6. **Deploy Kafka Connect Connectors** (optional, for Elasticsearch indexing):
   ```bash
   # Step 1: Install OpenSearch connector plugin
   # Download from: https://github.com/opensearch-project/opensearch-kafka-connect/releases
   # Place JAR in: docker/kafka-connect/plugins/opensearch-sink/lib/
   # Or use: ./scripts/download-opensearch-connector.sh <version>
   
   # Step 2: Restart Kafka Connect to load plugin
   docker compose restart kafka-connect
   
   # Step 3: Deploy all connectors
   ./scripts/deploy-connectors.sh
   
   # Step 4: Check connector health
   ./scripts/check-connectors.sh
   ```

### Running the Demo Scenario

The project includes a comprehensive demo script that exercises the entire system through a realistic police incident management scenario. This is the easiest way to see the system in action:

```bash
# Run the complete demo scenario
./scripts/demo-scenario.sh
```

**What the demo does:**
1. **Automatically starts all services** using Docker Compose (edge service + 3 projection services + infrastructure)
2. **Creates initial resources**: Officers, vehicles, units, locations, and persons
3. **Runs a complete incident workflow**:
   - Starts a shift and checks in officers
   - Receives a call and reports an incident
   - Dispatches units and creates assignments
   - Creates activities and involves parties
   - Completes activities and clears the incident
4. **Queries projections** to verify data was processed correctly
5. **Documents all created resources** for reference

**Output and Logging:**
- All output is automatically logged to: `/tmp/police-demo-YYYYMMDD-HHMMSS.log`
- Created resource IDs are stored in: `/tmp/police-demo-data.json`
- The log file location is displayed at the start and end of the script
- If the script fails, check the log file for detailed error messages

**Environment Variables:**
- `START_SERVICES=true` (default) - Automatically start Docker services
- `STOP_SERVICES=false` (default) - Keep services running after demo
- `EDGE_BASE_URL` - Override edge service URL (default: http://localhost:8080/api/v1)
- `DOCKER_COMPOSE_FILE` - Override compose file (default: docker-compose-integration.yml)

**Examples:**
```bash
# Run demo without starting services (assumes services already running)
START_SERVICES=false ./scripts/demo-scenario.sh

# Run demo and stop services when done
STOP_SERVICES=true ./scripts/demo-scenario.sh

# Use custom URLs
EDGE_BASE_URL=http://localhost:8080/api/v1 ./scripts/demo-scenario.sh
```

**Prerequisites:**
- Docker and Docker Compose installed
- `curl` installed (required)
- `jq` installed (optional, for better JSON formatting)

The demo script uses `docker-compose-integration.yml` which includes all infrastructure services (Kafka, NATS, PostgreSQL) plus the application services (edge + 3 projections) in a single compose file for easy testing.

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
- [Event Bus](doc/architecture/event-bus.md) - Kafka and NATS/JetStream event buses
- [Data Flow](doc/architecture/data-flow.md) - How data flows through the system
- [Kafka Connect Elasticsearch](doc/architecture/kafka-connect-elasticsearch.md) - Event indexing to Elasticsearch
- [Testing Strategy](doc/architecture/testing.md) - Testing principles, patterns, and practices

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
- **Edge servers** receive HTTP requests (commands) and produce events using a double-publish pattern
- **Events represent requests/commands** from the edge, not state changes
- **All events** are published to **Kafka** for event sourcing and long-term storage
- **Critical events** (ending in "Requested") are also published to **NATS/JetStream** for near realtime processing
- **No state reconstruction** in the edge layer - events are simply produced to event buses
- **All operations are asynchronous** via event buses

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
- **Primary Event Bus**: Apache Kafka (event sourcing and long-term storage)
- **Secondary Event Bus**: NATS/JetStream (critical events for near realtime processing)
- **Build Tool**: Maven
- **Testing**: JUnit 5, Kafka Test Containers

## Project Structure

```
policesystem/
├── common/              # Shared code (events, base classes)
├── edge/                # Edge server (REST API, command handlers)
│   └── Dockerfile       # Docker image for edge service
├── operational-projection/  # Operational projection service
│   └── Dockerfile       # Docker image for operational projection
├── resource-projection/     # Resource projection service
│   └── Dockerfile       # Docker image for resource projection
├── workforce-projection/    # Workforce projection service
│   └── Dockerfile       # Docker image for workforce projection
├── scripts/             # Utility scripts
│   ├── demo-scenario.sh # Complete demo scenario script
│   ├── deploy-connectors.sh
│   ├── check-connectors.sh
│   └── ...              # Other utility scripts
├── doc/                 # Documentation
│   ├── api/            # API documentation
│   ├── architecture/   # Architecture documentation
│   ├── domainmodel/   # Domain model documentation
│   └── events/        # Event specifications
├── docker-compose.yml  # Infrastructure services (Kafka, NATS, PostgreSQL, etc.)
├── docker-compose-integration.yml  # Full stack for integration testing/demo
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

## Event Distribution

### Kafka Topics

All events are published to domain-specific Kafka topics:
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

### NATS JetStream Subjects

Critical events (all command events ending in "Requested") are also published to NATS JetStream subjects following the pattern `commands.{domain}.{action}`:
- `commands.officer.register`, `commands.officer.update`, `commands.officer.change-status`
- `commands.vehicle.register`, `commands.vehicle.update`, `commands.vehicle.change-status`
- `commands.unit.create`, `commands.unit.update`, `commands.unit.change-status`
- `commands.person.register`, `commands.person.update`
- `commands.location.create`, `commands.location.update`
- `commands.incident.report`, `commands.incident.dispatch`, etc.
- And all other critical command events...

This double-publish pattern ensures:
- **Kafka**: Event sourcing, long-term storage, and eventual consistency
- **NATS/JetStream**: Low-latency delivery for near realtime processing of critical events

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

### Docker Integration

The project includes Docker support for all services:

**Build Docker images:**
```bash
# Build all services
docker compose -f docker-compose-integration.yml build

# Build specific service
docker compose -f docker-compose-integration.yml build edge-service
```

**Run services with Docker:**
```bash
# Start all services (infrastructure + applications)
docker compose -f docker-compose-integration.yml up -d

# View logs
docker compose -f docker-compose-integration.yml logs -f edge-service

# Stop all services
docker compose -f docker-compose-integration.yml down
```

**Service Ports:**
- Edge Service: `8080`
- Operational Projection: `8081`
- Resource Projection: `8082`
- Workforce Projection: `8083`
- PostgreSQL: `5432`
- Kafka: `9092-9094`
- NATS: `4222-4224`

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
