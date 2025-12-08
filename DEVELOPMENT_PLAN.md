# Police System Development Plan

This document outlines the complete development plan for the Police Incident Management System, organized into clear increments following the 8-step development process defined in [AGENTS.md](AGENTS.md).

## System Overview

The Police System is an Event-Driven system built with:
- **Language**: Java 17
- **Framework**: Spring Framework / Spring Boot
- **Primary Event Bus**: Apache Kafka (for event sourcing and long-term storage)
- **Secondary Event Bus**: NATS/JetStream (for critical messages requiring near realtime processing)
- **Architecture**: Event-Driven Edge Layer with Double-Publish Pattern (events represent requests/commands)

## Development Approach

### Core Principles
- **Edge servers** receive HTTP requests and produce events using double-publish pattern
- **Events represent requests/commands**, not state changes
- **No state reconstruction** in edge layer
- **No CQRS projections** in initial development
- **Tests verify event production** - call API, verify event in both Kafka and NATS/JetStream (for critical events)

### Double-Publish Pattern
The system implements a **double-publish pattern** for event distribution:
- **All events** are published to **Kafka** for event sourcing, long-term storage, and eventual consistency
- **Critical events** (commands and near realtime processing requirements) are also published to **NATS/JetStream** for:
  - Low-latency message delivery
  - At-least-once delivery guarantees
  - Real-time processing capabilities
  - Stream processing with JetStream

**Critical Events** include:
- All command events (e.g., `RegisterOfficerRequested`, `ReportIncidentRequested`)
- Status change events requiring immediate processing
- Dispatch and assignment events
- Any event requiring near realtime processing

**Non-Critical Events** (Kafka only):
- Update events that don't require immediate processing
- Historical events
- Events used primarily for analytics and reporting

### Event Naming
Events use request-based naming:
- `RegisterOfficerRequested` (not `OfficerRegistered`)
- `UpdateOfficerRequested` (not `OfficerUpdated`)
- `ChangeOfficerStatusRequested` (not `OfficerStatusChanged`)
- `ReportIncidentRequested` (not `IncidentReported`)
- `DispatchIncidentRequested` (not `IncidentDispatched`)

## Development Increments

### Phase 1: Foundation and Infrastructure

#### Increment 1.1: Project Setup and Build Configuration
**Status**: ✅ Completed

**Step 0: Requirements**
- Configure Maven parent POM with Java 17, Spring Boot dependencies
- Set up common module with shared dependencies (Kafka, Jackson, etc.)
- Set up edge module with Spring Boot Web and Kafka dependencies
- Configure test dependencies (JUnit 5, Mockito, Testcontainers for Kafka)
- Set up code quality tools (Checkstyle, SpotBugs)
- Configure build plugins (Maven Compiler, Surefire, etc.)

**Test Criteria**: 
- All modules compile successfully
- Test framework can run basic tests
- Kafka test containers can be started in tests

**Demo Suggestion**:
1. Show project structure (common, edge modules)
2. Show Maven POM configuration
3. Run `mvn clean test` to show test framework working
4. Show Kafka test container starting in a test

---

#### Increment 1.2: Event Base Classes and Infrastructure
**Status**: ✅ Completed

**Step 0: Requirements**
- Create base `Event` interface/abstract class with common fields (eventId, timestamp, aggregateId, version)
  - Location: `common` module, package `com.knowit.policesystem.common.events`
- Create event serialization/deserialization infrastructure (JSON)
  - Location: `common` module, package `com.knowit.policesystem.common.events`
- Implement event metadata handling
  - Location: `common` module, package `com.knowit.policesystem.common.events`
- Create event versioning support
  - Location: `common` module, package `com.knowit.policesystem.common.events`
- Set up event publishing infrastructure (Kafka producer wrapper)
  - Location: `common` module, package `com.knowit.policesystem.common.events`

**Test Criteria**:
- Event can be serialized to JSON
- Event can be deserialized from JSON
- Event contains required metadata (eventId, timestamp)
- Event can be published to Kafka
- Event versioning works correctly

**Implementation Details**:
- Created `Event` abstract base class with fields: eventId (UUID), timestamp (Instant), aggregateId (String), version (int)
- Implemented `EventPublisher` interface with methods for publishing events to event buses
- Implemented `KafkaEventPublisher` using KafkaProducer with JSON serialization
- Added `PublishCallback` interface to allow application programmers to handle both success and failure outcomes
- Added callback-based publish methods: `publish(String topic, Event event, PublishCallback callback)` and `publish(String topic, String key, Event event, PublishCallback callback)`
- Maintained backward compatibility: existing fire-and-forget `publish()` methods use default logging callback
- Added Jackson JSR310 module for Java 8 time support (Instant serialization)
- Configured ObjectMapper to serialize dates as ISO-8601 strings
- All tests passing: 7 tests covering serialization, deserialization, metadata, Kafka publishing, versioning, and callback handling
- Note: NATS/JetStream support will be added in Increment 1.5

**Demo Suggestion**:
1. Show base Event abstract class (`com.knowit.policesystem.common.events.Event`)
   - Highlight common fields: eventId, timestamp, aggregateId, version
   - Show abstract `getEventType()` method
2. Create a sample event extending base class (use TestEvent from tests)
   ```java
   TestEvent event = new TestEvent("aggregate-123", "test data", 42);
   ```
3. Serialize event to JSON using ObjectMapper
   ```java
   ObjectMapper mapper = new ObjectMapper();
   mapper.registerModule(new JavaTimeModule());
   mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
   String json = mapper.writeValueAsString(event);
   ```
   Show JSON output with ISO-8601 timestamp format
4. Publish event to Kafka using KafkaEventPublisher
   ```java
   EventPublisher publisher = new KafkaEventPublisher(producerProps, mapper);
   // Fire-and-forget (uses default logging callback)
   publisher.publish("test-events", "aggregate-123", event);
   
   // With callback to handle success/failure
   publisher.publish("test-events", "aggregate-123", event, new EventPublisher.PublishCallback() {
       @Override
       public void onSuccess(Event event, RecordMetadata metadata) {
           // Handle successful publication
       }
       
       @Override
       public void onFailure(Event event, Exception exception) {
           // Handle publication failure (retry, store for later, etc.)
       }
   });
   ```
5. Consume event from Kafka topic using kafka-console-consumer
   ```bash
   kafka-console-consumer --bootstrap-server localhost:9092 --topic test-events --from-beginning
   ```
   Show the JSON event structure in Kafka
6. Highlight event metadata (eventId, timestamp, aggregateId, version) in the JSON
7. Show event versioning by creating events with different versions

---

#### Increment 1.3: Command and Query Base Infrastructure
**Status**: ✅ Completed

**Step 0: Requirements**
- Create base `Command` interface/abstract class
  - Location: `edge` module, package `com.knowit.policesystem.edge.commands`
- Create base `Query` interface/abstract class
  - Location: `edge` module, package `com.knowit.policesystem.edge.queries`
- Implement command handler infrastructure
  - Location: `edge` module, package `com.knowit.policesystem.edge.commands`
- Implement query handler infrastructure
  - Location: `edge` module, package `com.knowit.policesystem.edge.queries`
- Create command/query validation framework
  - Location: `edge` module, package `com.knowit.policesystem.edge.validation`
- Set up error handling and response structures (DTOs)
  - Location: `edge` module, package `com.knowit.policesystem.edge.dto`

**Test Criteria**:
- Command can be validated
- Command handler can process commands
- Query handler can process queries
- Validation errors return appropriate HTTP status codes
- Error responses follow consistent structure

**Implementation Details**:
- Created `Command` abstract base class with fields: commandId (UUID), timestamp (Instant), aggregateId (String)
- Created `Query` abstract base class with fields: queryId (UUID), timestamp (Instant)
- Implemented `CommandHandler` interface with generic type parameters
- Implemented `CommandHandlerRegistry` Spring component for handler registration and lookup
- Implemented `QueryHandler` interface with generic type parameters
- Created validation framework: `Validator` interface, `ValidationResult`, `ValidationError`, `CommandValidator` base class
- Created DTOs: `ErrorResponse`, `SuccessResponse`, `ValidationErrorResponse`
- Created exceptions: `ValidationException`, `CommandHandlerNotFoundException`, `QueryHandlerNotFoundException`
- Implemented `GlobalExceptionHandler` with `@ControllerAdvice` for centralized exception handling
- All tests passing: 39 tests covering all components

**Demo Suggestion**:
1. Show base Command and Query abstract classes
   - Highlight common fields: commandId, timestamp, aggregateId (for Command)
   - Show abstract `getCommandType()` and `getQueryType()` methods
2. Create a sample command and handler
   ```java
   TestCommand command = new TestCommand("aggregate-123", "test data", 42);
   CommandHandler<TestCommand, String> handler = new TestCommandHandler();
   String result = handler.handle(command);
   ```
3. Show validation framework in action
   ```java
   CommandValidator validator = new TestCommandValidator();
   ValidationResult result = validator.validate(command);
   if (!result.isValid()) {
       result.getErrors().forEach(error -> 
           System.out.println(error.getField() + ": " + error.getMessage()));
   }
   ```
4. Show error response structure
   ```java
   ErrorResponse error = new ErrorResponse("Bad Request", "Validation failed", 
       List.of("field1 is required", "field2 must be valid email"));
   ValidationErrorResponse validationError = ValidationErrorResponse.fromValidationResult(validationResult);
   ```
5. Show GlobalExceptionHandler handling exceptions
   - Demonstrate ValidationException → 400 Bad Request
   - Demonstrate CommandHandlerNotFoundException → 500 Internal Server Error
   - Show consistent error response format

---

#### Increment 1.4: REST API Infrastructure
**Status**: ✅ Completed

---

#### Increment 1.5: NATS/JetStream Infrastructure
**Status**: ✅ Completed

**Step 0: Requirements**
- Add NATS Java client dependency to common module
- Create `NatsEventPublisher` implementing `EventPublisher` interface
  - Location: `common` module, package `com.knowit.policesystem.common.events`
- Implement JetStream publishing with:
  - Subject-based routing (e.g., `commands.officer.register`, `commands.incident.report`)
  - At-least-once delivery guarantees
  - Message acknowledgment handling
  - Error handling and retry logic
- Create `DualEventPublisher` composite that publishes to both Kafka and NATS/JetStream
  - Location: `common` module, package `com.knowit.policesystem.common.events`
  - Determines if event is critical and should be published to NATS/JetStream
  - Always publishes to Kafka
  - Publishes to NATS/JetStream for critical events (commands and near realtime events)
- Configure NATS connection in Spring Boot
  - Location: `edge` module, package `com.knowit.policesystem.edge.config`
- Add NATS test container support for testing
- Update `EventPublisher` interface to support dual publishing (if needed)
- Create event classification mechanism to determine critical vs non-critical events

**Test Criteria**:
- `NatsEventPublisher` can publish events to NATS JetStream subjects
- Events are correctly serialized to JSON
- Message acknowledgment works correctly
- Error handling and retry logic function properly
- `DualEventPublisher` publishes all events to Kafka
- `DualEventPublisher` publishes critical events to NATS/JetStream
- `DualEventPublisher` skips NATS/JetStream for non-critical events
- Events can be consumed from NATS JetStream subjects
- NATS test containers can be used in integration tests
- Both Kafka and NATS/JetStream events contain correct data

**Implementation Details**:
- Use `io.nats:jnats` Java client library for NATS connectivity
- Use JetStream API for persistent message storage
- Subject naming convention: `commands.{domain}.{action}` (e.g., `commands.officer.register`)
- Event classification: All command events (ending in `Requested`) are considered critical
- DualEventPublisher uses composition pattern with KafkaEventPublisher and NatsEventPublisher
- Configuration allows enabling/disabling NATS publishing per environment
- Both publishers use the same ObjectMapper for JSON serialization consistency

**Demo Suggestion**:
1. Show NATS/JetStream configuration in application.yml
2. Show DualEventPublisher implementation and event classification logic
3. Publish a critical event (e.g., `ReportIncidentRequested`)
4. Verify event appears in both Kafka topic and NATS JetStream subject
   ```bash
   # Kafka
   kafka-console-consumer --bootstrap-server localhost:9092 --topic incident-events --from-beginning
   
   # NATS JetStream
   nats stream view commands.incident.report
   ```
5. Publish a non-critical event and show it only goes to Kafka
6. Show NATS JetStream message acknowledgment and delivery guarantees
7. Explain the double-publish pattern and when to use each event bus

---

**Step 0: Requirements**
- Set up Spring Web MVC configuration
- Create REST controller base classes
- Implement request/response DTOs
- Set up API versioning
- Integrate existing API documentation (OpenAPI/Swagger) with Spring Boot
  - API specification already exists at `doc/api/openapi.yaml`
  - Integrate with springdoc-openapi-ui to serve Swagger UI
  - Configure Spring Boot to use the existing OpenAPI specification
- Implement error handling and exception mapping
- Add request validation

**Test Criteria**:
- REST endpoints can be called
- Request validation works
- Error responses are properly formatted
- API documentation is accessible via Swagger UI
- Exception handling returns appropriate status codes

**Implementation Details**:
- Integrated OpenAPI YAML specification file into SpringDoc
  - Copied openapi.yaml to `edge/src/main/resources/api/` for classpath access
  - Updated `OpenApiConfig` to load OpenAPI spec from YAML file using swagger-parser
  - Added swagger-parser-v3 dependency for YAML parsing
- Created comprehensive REST API infrastructure tests
  - `RestApiInfrastructureTest` with 25 test cases covering:
    * Swagger UI accessibility and API documentation endpoints
    * Request validation with @Valid annotations
    * Error handling for all exception types
    * Content negotiation (JSON default)
    * API versioning consistency (/api/v1)
    * BaseRestController helper methods
- Added test infrastructure components
  - `TestRestController` with test endpoints (test profile only)
  - `TestRequestDto` with validation annotations for testing
- Verified API versioning consistency
  - `BaseRestController` uses /api/v1 base path
  - `OpenApiConfig` GroupedOpenApi matches /api/v1/** paths
  - All endpoints consistently use versioned paths
- All 72 tests passing (25 new infrastructure tests + 47 existing tests)

**Demo Suggestion**:
1. Show Spring Boot application starting
2. Show Swagger UI with API documentation (served from existing openapi.yaml)
3. Make a test API call (even if it fails)
4. Show error response structure
5. Show request validation in action
6. Highlight that API documentation was created upfront and is now integrated

---

### Phase 2: PoliceOfficer Domain

#### Increment 2.1: Report Incident Endpoint
**Status**: ✅ Completed

**Step 0: Requirements**
- REST API: `POST /api/incidents`
- Request body: `{ incidentId, incidentNumber, priority, status, reportedTime, description, incidentType }`
- Response: `201 Created` with `{ incidentId, incidentNumber }`
- Produces event: `ReportIncidentRequested` to Kafka topic `incident-events` and NATS JetStream subject `commands.incident.report`
- Validation: incidentId required, priority enum, status enum, incidentType enum
- Test criteria: Verify `ReportIncidentRequested` event appears in both Kafka and NATS/JetStream with correct data

**Test Criteria**:
- `testReportIncident_WithValidData_ProducesEvent()` - Call POST /api/incidents, verify event in both Kafka and NATS/JetStream
- `testReportIncident_WithMissingIncidentId_Returns400()` - Validation error, no event in either bus
- `testReportIncident_WithInvalidPriority_Returns400()` - Priority enum validation, no event in either bus
- `testReportIncident_WithInvalidStatus_Returns400()` - Status enum validation, no event in either bus
- Event contains all incident data (incidentId, incidentNumber, priority, status, reportedTime, description, incidentType)
- Event has eventId, timestamp, and aggregateId (incidentId)
- Event appears in Kafka topic `incident-events`
- Event appears in NATS JetStream subject `commands.incident.report` (critical event)

**Implementation Details**:
- Created domain enums: `Priority`, `IncidentStatus`, `IncidentType` in `edge/src/main/java/com/knowit/policesystem/edge/domain/`
- Created DTOs: `ReportIncidentRequestDto` and `IncidentResponseDto` with validation annotations (`@NotBlank`, `@NotNull`)
- Created `ReportIncidentCommand` extending base `Command` class in `edge/src/main/java/com/knowit/policesystem/edge/commands/incidents/`
- Created `ReportIncidentRequested` event extending base `Event` class in `common/src/main/java/com/knowit/policesystem/common/events/incidents/`
- Created `ReportIncidentCommandValidator` with validation for required fields and enum values
- Created `ReportIncidentCommandHandler` that publishes events to Kafka topic "incident-events"
- Created `IncidentController` with POST `/api/v1/incidents` endpoint extending `BaseRestController`
- Configured `EventPublisher`/`KafkaEventPublisher` as Spring bean in `KafkaConfig` with proper ObjectMapper configuration
- Enhanced `GlobalExceptionHandler` to handle `HttpMessageNotReadableException` for invalid enum values (returns 400 Bad Request)
- Created comprehensive integration tests in `IncidentControllerTest` with Kafka test containers
- All components follow event-driven architecture pattern: REST Controller → Command → Command Handler → Event Publisher → Kafka Topic
- Event uses request-based naming: `ReportIncidentRequested` (not `IncidentReported`)
- Validation occurs at both DTO level (via `@Valid`) and command level (via `CommandValidator`)
- Tests verify Kafka event production with proper event structure and metadata

**Demo Suggestion**:
1. Show POST /api/incidents request with curl or Postman
   ```bash
   curl -X POST http://localhost:8080/api/incidents \
     -H "Content-Type: application/json" \
     -d '{"incidentId":"INC-001","incidentNumber":"2024-001","priority":"High","status":"Reported","reportedTime":"2024-01-15T10:30:00Z","description":"Traffic accident at Main St and Oak Ave","incidentType":"Traffic"}'
   ```
2. Show 201 Created response
3. Show ReportIncidentRequested event in Kafka topic using kafka-console-consumer
   ```bash
   kafka-console-consumer --bootstrap-server localhost:9092 --topic incident-events --from-beginning
   ```
4. Show ReportIncidentRequested event in NATS JetStream subject
   ```bash
   nats stream view commands.incident.report
   ```
5. Highlight event structure (eventId, timestamp, aggregateId, event data) in both buses
6. Show validation error example (missing incidentId) - 400 Bad Request, no events published
7. Show incident priorities and types
8. Explain double-publish pattern: Kafka for event sourcing, NATS/JetStream for near realtime processing

---

#### Increment 2.2: Register Officer Endpoint
**Status**: ✅ Completed

**Step 0: Requirements**
- REST API: `POST /api/v1/officers`
- Request body: `{ badgeNumber, firstName, lastName, rank, email, phoneNumber, hireDate, status }`
- Response: `201 Created` with `{ officerId, badgeNumber }`
- Produces event: `RegisterOfficerRequested` to Kafka topic `officer-events` and NATS JetStream subject `commands.officer.register`
- Validation: badgeNumber required, email format, status enum
- Test criteria: Verify `RegisterOfficerRequested` event appears in both Kafka and NATS/JetStream with correct data

**Test Criteria**:
- `testRegisterOfficer_WithValidData_ProducesEvent()` - Call POST /api/v1/officers, verify event in both Kafka and NATS/JetStream
- `testRegisterOfficer_WithMissingBadgeNumber_Returns400()` - Validation error, no event in either bus
- `testRegisterOfficer_WithInvalidEmail_Returns400()` - Email validation, no event in either bus
- `testRegisterOfficer_WithInvalidStatus_Returns400()` - Status enum validation, no event in either bus
- `testRegisterOfficer_WithEmptyBadgeNumber_Returns400()` - Empty badgeNumber validation, no event in either bus
- Event contains all officer data (badgeNumber, firstName, lastName, rank, email, phoneNumber, hireDate, status)
- Event has eventId, timestamp, and aggregateId (badgeNumber)
- Event appears in Kafka topic `officer-events`
- Event appears in NATS JetStream subject `commands.officer.register` (critical event)

**Implementation Details**:
- Created domain enum: `OfficerStatus` (Active, OnDuty, OffDuty, Suspended, Retired) in `edge/src/main/java/com/knowit/policesystem/edge/domain/`
- Created DTOs: `RegisterOfficerRequestDto` and `OfficerResponseDto` with validation annotations (`@NotBlank`, `@Email`, `@NotNull`)
- Created `RegisterOfficerCommand` extending base `Command` class in `edge/src/main/java/com/knowit/policesystem/edge/commands/officers/`
- Created `RegisterOfficerRequested` event extending base `Event` class in `common/src/main/java/com/knowit/policesystem/common/events/officers/`
- Created `RegisterOfficerCommandValidator` with validation for required fields (badgeNumber, email format, status enum)
- Created `RegisterOfficerCommandHandler` that publishes events to Kafka topic "officer-events"
- Created `OfficerController` with POST `/api/v1/officers` endpoint extending `BaseRestController`
- DualEventPublisher automatically publishes critical events (ending in "Requested") to both Kafka and NATS/JetStream
- All components follow event-driven architecture pattern: REST Controller → Command → Command Handler → Event Publisher → Kafka Topic (and NATS/JetStream)
- Event uses request-based naming: `RegisterOfficerRequested` (not `OfficerRegistered`)
- Validation occurs at both DTO level (via `@Valid`) and command level (via `CommandValidator`)
- Created comprehensive integration tests in `OfficerControllerTest` with Kafka test containers (5 test cases, all passing)
- Tests verify Kafka event production with proper event structure and metadata
- Note: NATS is disabled in test profile, but DualEventPublisher infrastructure is in place for production use

**Demo Suggestion**:
1. Show POST /api/v1/officers request with curl or Postman
   ```bash
   curl -X POST http://localhost:8080/api/v1/officers \
     -H "Content-Type: application/json" \
     -d '{"badgeNumber":"12345","firstName":"John","lastName":"Doe","rank":"Officer","email":"john.doe@police.gov","phoneNumber":"555-0100","hireDate":"2020-01-15","status":"Active"}'
   ```
2. Show 201 Created response
3. Show RegisterOfficerRequested event in Kafka topic using kafka-console-consumer
   ```bash
   kafka-console-consumer --bootstrap-server localhost:9092 --topic officer-events --from-beginning
   ```
4. Show RegisterOfficerRequested event in NATS JetStream subject
   ```bash
   nats stream view commands.officer.register
   ```
5. Highlight event structure (eventId, timestamp, aggregateId, event data) in both buses
6. Show validation error example (missing badgeNumber) - 400 Bad Request, no events published
7. Explain double-publish pattern: Kafka for event sourcing, NATS/JetStream for near realtime processing

---

#### Increment 2.3: Update Officer Endpoint
**Status**: ✅ Completed

**Step 0: Requirements**
- REST API: `PUT /api/v1/officers/{badgeNumber}`
- Request body: `{ firstName, lastName, rank, email, phoneNumber, hireDate }` (all optional)
- Response: `200 OK` with `SuccessResponse<OfficerResponseDto>`
- Produces event: `UpdateOfficerRequested` to Kafka topic `officer-events` and NATS JetStream subject `commands.officer.update`
- Validation: badgeNumber format, email format if provided
- Test criteria: Verify `UpdateOfficerRequested` event appears in both Kafka and NATS/JetStream

**Test Criteria**:
- `testUpdateOfficer_WithValidData_ProducesEvent()` - Call PUT /api/v1/officers/{badgeNumber}, verify event
- `testUpdateOfficer_WithAllFields_ProducesEvent()` - All fields provided, verify all fields in event
- `testUpdateOfficer_WithOnlyFirstName_ProducesEvent()` - Partial update, verify only provided fields
- `testUpdateOfficer_WithInvalidEmail_Returns400()` - Email validation, no event
- Event contains badgeNumber and only provided fields (nulls for omitted fields)
- Event has eventId, timestamp, aggregateId

**Implementation Details**:
- Created `UpdateOfficerRequestDto` with all optional fields (firstName, lastName, rank, email, phoneNumber, hireDate) matching OpenAPI spec
- Created `UpdateOfficerCommand` extending base `Command` class in `edge/src/main/java/com/knowit/policesystem/edge/commands/officers/`
- Created `UpdateOfficerRequested` event extending base `Event` class in `common/src/main/java/com/knowit/policesystem/common/events/officers/`
- Created `UpdateOfficerCommandValidator` with validation for badgeNumber format and email format if provided
- Created `UpdateOfficerCommandHandler` that publishes events to Kafka topic "officer-events"
- Added PUT endpoint to `OfficerController` with path `/api/v1/officers/{badgeNumber}`
- DualEventPublisher automatically publishes critical events (ending in "Requested") to both Kafka and NATS/JetStream
- All components follow event-driven architecture pattern: REST Controller → Command → Command Handler → Event Publisher → Kafka Topic (and NATS/JetStream)
- Event uses request-based naming: `UpdateOfficerRequested` (not `OfficerUpdated`)
- Validation occurs at both DTO level (via `@Valid` and `@Email`) and command level (via `CommandValidator`)
- Created comprehensive integration tests in `OfficerControllerTest` with Kafka test containers (5 new test cases, all passing)
- Tests verify Kafka event production with proper event structure and metadata
- Partial updates supported - event contains only provided fields, nulls for omitted fields
- Note: NATS is disabled in test profile, but DualEventPublisher infrastructure is in place for production use

**Demo Suggestion**:
1. Show PUT /api/v1/officers/12345 request with curl or Postman
   ```bash
   curl -X PUT http://localhost:8080/api/v1/officers/12345 \
     -H "Content-Type: application/json" \
     -d '{"firstName":"Jane","lastName":"Smith","rank":"Sergeant","email":"jane.smith@police.gov"}'
   ```
2. Show 200 OK response
3. Show UpdateOfficerRequested event in Kafka topic using kafka-console-consumer
   ```bash
   kafka-console-consumer --bootstrap-server localhost:9092 --topic officer-events --from-beginning
   ```
4. Show UpdateOfficerRequested event in NATS JetStream subject
   ```bash
   nats stream view commands.officer.update
   ```
5. Highlight event structure (eventId, timestamp, aggregateId, event data) in both buses
6. Show partial update example (only firstName provided) - verify other fields are null in event
7. Show validation error example (invalid email format) - 400 Bad Request, no events published
8. Explain double-publish pattern: Kafka for event sourcing, NATS/JetStream for near realtime processing

---

#### Increment 2.4: Change Officer Status Endpoint
**Status**: ✅ Completed

**Step 0: Requirements**
- REST API: `PATCH /api/v1/officers/{badgeNumber}` (note: path follows OpenAPI spec, not `/status` sub-path)
- Request body: `{ status }`
- Response: `200 OK` with `{ badgeNumber, status }`
- Produces event: `ChangeOfficerStatusRequested` to Kafka topic `officer-events` and NATS JetStream subject `commands.officer.change-status`
- Validation: badgeNumber must exist, status must be valid enum
- Test criteria: Verify `ChangeOfficerStatusRequested` event appears in both Kafka and NATS/JetStream

**Test Criteria**:
- `testChangeOfficerStatus_WithValidStatus_ProducesEvent()` - Call PATCH endpoint, verify event
- `testChangeOfficerStatus_WithInvalidStatus_Returns400()` - Invalid status enum, no event
- `testChangeOfficerStatus_WithMissingStatus_Returns400()` - Missing status field, no event
- `testChangeOfficerStatus_WithEmptyBadgeNumber_Returns400()` - Empty badgeNumber in path, no event
- Event contains badgeNumber and new status
- Event has eventId, timestamp, aggregateId

**Implementation Details**:
- Created DTOs: `ChangeOfficerStatusRequestDto` with `@NotNull` status field and `OfficerStatusResponseDto` with badgeNumber and status fields
- Created `ChangeOfficerStatusCommand` extending base `Command` class in `edge/src/main/java/com/knowit/policesystem/edge/commands/officers/`
- Created `ChangeOfficerStatusRequested` event extending base `Event` class in `common/src/main/java/com/knowit/policesystem/common/events/officers/`
- Created `ChangeOfficerStatusCommandValidator` with validation for badgeNumber (required) and status (required, valid OfficerStatus enum: Active, OnDuty, OffDuty, Suspended, Retired)
- Created `ChangeOfficerStatusCommandHandler` that publishes events to Kafka topic "officer-events"
- Added PATCH endpoint to `OfficerController` with path `/api/v1/officers/{badgeNumber}` (following OpenAPI spec, not `/status` sub-path)
- DualEventPublisher automatically publishes critical events (ending in "Requested") to both Kafka and NATS/JetStream
- All components follow event-driven architecture pattern: REST Controller → Command → Command Handler → Event Publisher → Kafka Topic (and NATS/JetStream)
- Event uses request-based naming: `ChangeOfficerStatusRequested` (not `OfficerStatusChanged`)
- Validation occurs at both DTO level (via `@Valid` and `@NotNull`) and command level (via `CommandValidator`)
- Created comprehensive integration tests in `OfficerControllerTest` with Kafka test containers (4 test cases, all passing)
- Tests verify Kafka event production with proper event structure and metadata
- Note: NATS is disabled in test profile, but DualEventPublisher infrastructure is in place for production use
- Note: Endpoint path follows OpenAPI spec (`/api/v1/officers/{badgeNumber}` with PATCH method) rather than `/api/v1/officers/{badgeNumber}/status` as originally mentioned in requirements

**Demo Suggestion**:
1. Show PATCH /api/v1/officers/12345 request with curl or Postman
   ```bash
   curl -X PATCH http://localhost:8080/api/v1/officers/12345 \
     -H "Content-Type: application/json" \
     -d '{"status":"OnDuty"}'
   ```
2. Show 200 OK response with badgeNumber and status
3. Show ChangeOfficerStatusRequested event in Kafka topic using kafka-console-consumer
   ```bash
   kafka-console-consumer --bootstrap-server localhost:9092 --topic officer-events --from-beginning
   ```
4. Show ChangeOfficerStatusRequested event in NATS JetStream subject
   ```bash
   nats stream view commands.officer.change-status
   ```
5. Highlight event structure (eventId, timestamp, aggregateId, event data) in both buses
6. Show validation error example (invalid status enum value) - 400 Bad Request, no events published
7. Show validation error example (missing status field) - 400 Bad Request, no events published
8. Explain status enum values (Active, OnDuty, OffDuty, Suspended, Retired)
9. Explain double-publish pattern: Kafka for event sourcing, NATS/JetStream for near realtime processing

---

### Phase 3: PoliceVehicle Domain

#### Increment 3.1: Register Vehicle Endpoint
**Status**: ✅ Completed

**Step 0: Requirements**
- REST API: `POST /api/v1/vehicles`
- Request body: `{ unitId, vehicleType, licensePlate, vin, status, lastMaintenanceDate }`
- Response: `201 Created` with `{ vehicleId, unitId }`
- Produces event: `RegisterVehicleRequested` to Kafka topic `vehicle-events` and NATS JetStream subject `commands.vehicle.register`
- Validation: unitId required, VIN format (17 characters, alphanumeric excluding I, O, Q), status enum, vehicleType enum
- Test criteria: Verify `RegisterVehicleRequested` event appears in both Kafka and NATS/JetStream with correct data

**Test Criteria**:
- `testRegisterVehicle_WithValidData_ProducesEvent()` - Call POST /api/v1/vehicles, verify event in both Kafka and NATS/JetStream
- `testRegisterVehicle_WithMissingUnitId_Returns400()` - Validation error, no event in either bus
- `testRegisterVehicle_WithInvalidVIN_Returns400()` - VIN validation (wrong length), no event in either bus
- `testRegisterVehicle_WithInvalidVIN_ContainsInvalidCharacters_Returns400()` - VIN validation (contains I/O/Q), no event in either bus
- `testRegisterVehicle_WithInvalidStatus_Returns400()` - Status enum validation, no event in either bus
- `testRegisterVehicle_WithInvalidVehicleType_Returns400()` - VehicleType enum validation, no event in either bus
- `testRegisterVehicle_WithEmptyUnitId_Returns400()` - Empty unitId validation, no event in either bus
- `testRegisterVehicle_WithValidData_WithoutLastMaintenanceDate_ProducesEvent()` - Optional field handling
- Event contains all vehicle data (unitId, vehicleType, licensePlate, vin, status, lastMaintenanceDate)
- Event has eventId, timestamp, and aggregateId (unitId)
- Event appears in Kafka topic `vehicle-events`
- Event appears in NATS JetStream subject `commands.vehicle.register` (critical event)

**Implementation Details**:
- Created domain enums: `VehicleStatus` (Available, Assigned, InUse, Maintenance, OutOfService) and `VehicleType` (Patrol, SUV, Motorcycle, Van, Truck) in `edge/src/main/java/com/knowit/policesystem/edge/domain/`
- Created DTOs: `RegisterVehicleRequestDto` and `VehicleResponseDto` with validation annotations (`@NotBlank`, `@NotNull`)
- Created `RegisterVehicleCommand` extending base `Command` class in `edge/src/main/java/com/knowit/policesystem/edge/commands/vehicles/`
- Created `RegisterVehicleRequested` event extending base `Event` class in `common/src/main/java/com/knowit/policesystem/common/events/vehicles/`
- Created `RegisterVehicleCommandValidator` with validation for required fields (unitId, vin format, status enum, vehicleType enum)
  - VIN validation: regex pattern `^[A-HJ-NPR-Z0-9]{17}$` (17 characters, alphanumeric excluding I, O, Q)
- Created `RegisterVehicleCommandHandler` that publishes events to Kafka topic "vehicle-events"
- Created `VehicleController` with POST `/api/v1/vehicles` endpoint extending `BaseRestController`
- DualEventPublisher automatically publishes critical events (ending in "Requested") to both Kafka and NATS/JetStream
- All components follow event-driven architecture pattern: REST Controller → Command → Command Handler → Event Publisher → Kafka Topic (and NATS/JetStream)
- Event uses request-based naming: `RegisterVehicleRequested` (not `VehicleRegistered`)
- Validation occurs at both DTO level (via `@Valid`) and command level (via `CommandValidator`)
- Created comprehensive integration tests in `VehicleControllerTest` with Kafka test containers (8 test cases, all passing)
- Tests verify Kafka event production with proper event structure and metadata
- Note: NATS is disabled in test profile, but DualEventPublisher infrastructure is in place for production use

**Demo Suggestion**:
1. Show POST /api/v1/vehicles request with curl or Postman
   ```bash
   curl -X POST http://localhost:8080/api/v1/vehicles \
     -H "Content-Type: application/json" \
     -d '{"unitId":"UNIT-001","vehicleType":"Patrol","licensePlate":"ABC-123","vin":"1HGBH41JXMN109186","status":"Available","lastMaintenanceDate":"2024-01-15"}'
   ```
2. Show 201 Created response
3. Show RegisterVehicleRequested event in Kafka topic using kafka-console-consumer
   ```bash
   kafka-console-consumer --bootstrap-server localhost:9092 --topic vehicle-events --from-beginning
   ```
4. Show RegisterVehicleRequested event in NATS JetStream subject
   ```bash
   nats stream view commands.vehicle.register
   ```
5. Highlight event structure (eventId, timestamp, aggregateId, event data) in both buses
6. Show validation error example (missing unitId) - 400 Bad Request, no events published
7. Show validation error example (invalid VIN format) - 400 Bad Request, no events published
8. Show vehicle types and statuses
9. Explain double-publish pattern: Kafka for event sourcing, NATS/JetStream for near realtime processing

---

#### Increment 3.2: Update Vehicle Endpoint
**Status**: ✅ Completed

**Step 0: Requirements**
- REST API: `PUT /api/v1/vehicles/{unitId}`
- Request body: `{ vehicleType, licensePlate, vin, status, lastMaintenanceDate }` (all optional)
- Response: `200 OK` with `SuccessResponse<VehicleResponseDto>`
- Produces event: `UpdateVehicleRequested` to Kafka topic `vehicle-events` and NATS JetStream subject `commands.vehicle.update`
- Validation: unitId required (from path), VIN format if provided
- Test criteria: Verify `UpdateVehicleRequested` event appears in both Kafka and NATS/JetStream with correct data

**Test Criteria**:
- `testUpdateVehicle_WithValidData_ProducesEvent()` - Call PUT /api/v1/vehicles/{unitId}, verify event in Kafka
- `testUpdateVehicle_WithAllFields_ProducesEvent()` - All fields provided, verify all fields in event
- `testUpdateVehicle_WithOnlyVehicleType_ProducesEvent()` - Partial update (only vehicleType), verify nulls for other fields
- `testUpdateVehicle_WithInvalidVIN_Returns400()` - Invalid VIN format, verify 400 and no event
- `testUpdateVehicle_WithEmptyUnitId_Returns400()` - Empty unitId in path, verify error
- Event contains unitId and provided fields (nulls for omitted fields)
- Event has eventId, timestamp, aggregateId
- Event appears in Kafka topic `vehicle-events`
- Event appears in NATS JetStream subject `commands.vehicle.update` (critical event)

**Implementation Details**:
- Created `UpdateVehicleRequestDto` with all optional fields (vehicleType, licensePlate, vin, status, lastMaintenanceDate) matching OpenAPI spec
- Created `UpdateVehicleCommand` extending base `Command` class in `edge/src/main/java/com/knowit/policesystem/edge/commands/vehicles/`
- Created `UpdateVehicleRequested` event extending base `Event` class in `common/src/main/java/com/knowit/policesystem/common/events/vehicles/`
- Created `UpdateVehicleCommandValidator` with validation for unitId (required) and VIN format if provided (17 characters, pattern `^[A-HJ-NPR-Z0-9]{17}$`)
- Created `UpdateVehicleCommandHandler` that publishes events to Kafka topic "vehicle-events"
- Added PUT endpoint to `VehicleController` with path `/api/v1/vehicles/{unitId}`
- DualEventPublisher automatically publishes critical events (ending in "Requested") to both Kafka and NATS/JetStream
- All components follow event-driven architecture pattern: REST Controller → Command → Command Handler → Event Publisher → Kafka Topic (and NATS/JetStream)
- Event uses request-based naming: `UpdateVehicleRequested` (not `VehicleUpdated`)
- Validation occurs at both DTO level (via `@Valid`) and command level (via `CommandValidator`)
- Created comprehensive integration tests in `VehicleControllerTest` with Kafka test containers (5 new test cases, all passing)
- Tests verify Kafka event production with proper event structure and metadata
- Partial updates supported - event contains only provided fields, nulls for omitted fields
- Note: NATS is disabled in test profile, but DualEventPublisher infrastructure is in place for production use

**Demo Suggestion**:
1. Show PUT /api/v1/vehicles/UNIT-001 request with curl or Postman
   ```bash
   curl -X PUT http://localhost:8080/api/v1/vehicles/UNIT-001 \
     -H "Content-Type: application/json" \
     -d '{"vehicleType":"SUV","licensePlate":"XYZ-123","vin":"2HGBH41JXMN109186","status":"Maintenance","lastMaintenanceDate":"2024-02-20"}'
   ```
2. Show 200 OK response
3. Show UpdateVehicleRequested event in Kafka topic using kafka-console-consumer
   ```bash
   kafka-console-consumer --bootstrap-server localhost:9092 --topic vehicle-events --from-beginning
   ```
4. Show UpdateVehicleRequested event in NATS JetStream subject
   ```bash
   nats stream view commands.vehicle.update
   ```
5. Highlight event structure (eventId, timestamp, aggregateId, event data) in both buses
6. Show partial update example (only vehicleType provided) - verify other fields are null in event
7. Show validation error example (invalid VIN format) - 400 Bad Request, no events published
8. Explain double-publish pattern: Kafka for event sourcing, NATS/JetStream for near realtime processing

---

#### Increment 3.3: Change Vehicle Status Endpoint
**Status**: ✅ Completed

**Step 0: Requirements**
- REST API: `PATCH /api/v1/vehicles/{unitId}` (note: path follows OpenAPI spec, not `/status` sub-path)
- Request body: `{ status }`
- Response: `200 OK` with `SuccessResponse<VehicleStatusResponseDto>` containing `{ unitId, status }`
- Produces event: `ChangeVehicleStatusRequested` to Kafka topic `vehicle-events` and NATS JetStream subject `commands.vehicle.change-status`
- Validation: unitId required, status must be valid VehicleStatus enum (Available, Assigned, InUse, Maintenance, OutOfService)
- Test criteria: Verify `ChangeVehicleStatusRequested` event appears in both Kafka and NATS/JetStream

**Test Criteria**:
- `testChangeVehicleStatus_WithValidStatus_ProducesEvent()` - Call PATCH endpoint, verify event
- `testChangeVehicleStatus_WithInvalidStatus_Returns400()` - Invalid status enum, no event
- `testChangeVehicleStatus_WithMissingStatus_Returns400()` - Missing status field, no event
- `testChangeVehicleStatus_WithEmptyUnitId_Returns400()` - Empty unitId in path, no event
- Event contains unitId and new status
- Event has eventId, timestamp, aggregateId
- Event appears in Kafka topic `vehicle-events`
- Event appears in NATS JetStream subject `commands.vehicle.change-status` (critical event)

**Implementation Details**:
- Created DTOs: `ChangeVehicleStatusRequestDto` with `@NotNull` status field and `VehicleStatusResponseDto` with unitId and status fields
- Created `ChangeVehicleStatusCommand` extending base `Command` class in `edge/src/main/java/com/knowit/policesystem/edge/commands/vehicles/`
- Created `ChangeVehicleStatusRequested` event extending base `Event` class in `common/src/main/java/com/knowit/policesystem/common/events/vehicles/`
- Created `ChangeVehicleStatusCommandValidator` with validation for unitId (required) and status (required, valid VehicleStatus enum: Available, Assigned, InUse, Maintenance, OutOfService)
- Created `ChangeVehicleStatusCommandHandler` that publishes events to Kafka topic "vehicle-events"
- Added PATCH endpoint to `VehicleController` with path `/api/v1/vehicles/{unitId}` (following OpenAPI spec)
- DualEventPublisher automatically publishes critical events (ending in "Requested") to both Kafka and NATS/JetStream
- All components follow event-driven architecture pattern: REST Controller → Command → Command Handler → Event Publisher → Kafka Topic (and NATS/JetStream)
- Event uses request-based naming: `ChangeVehicleStatusRequested` (not `VehicleStatusChanged`)
- Validation occurs at both DTO level (via `@Valid` and `@NotNull`) and command level (via `CommandValidator`)
- Created comprehensive integration tests in `VehicleControllerTest` with Kafka test containers (4 new test cases, all passing)
- Tests verify Kafka event production with proper event structure and metadata
- Note: NATS is disabled in test profile, but DualEventPublisher infrastructure is in place for production use

**Demo Suggestion**:
1. Show PATCH /api/v1/vehicles/UNIT-001 request with curl or Postman
   ```bash
   curl -X PATCH http://localhost:8080/api/v1/vehicles/UNIT-001 \
     -H "Content-Type: application/json" \
     -d '{"status":"InUse"}'
   ```
2. Show 200 OK response with unitId and status
3. Show ChangeVehicleStatusRequested event in Kafka topic using kafka-console-consumer
   ```bash
   kafka-console-consumer --bootstrap-server localhost:9092 --topic vehicle-events --from-beginning
   ```
4. Show ChangeVehicleStatusRequested event in NATS JetStream subject
   ```bash
   nats stream view commands.vehicle.change-status
   ```
5. Highlight event structure (eventId, timestamp, aggregateId, event data) in both buses
6. Show validation error example (invalid status enum value) - 400 Bad Request, no events published
7. Show validation error example (missing status field) - 400 Bad Request, no events published
8. Explain status enum values (Available, Assigned, InUse, Maintenance, OutOfService)
9. Explain double-publish pattern: Kafka for event sourcing, NATS/JetStream for near realtime processing

---

### Phase 4: Unit Domain

#### Increment 4.1: Create Unit Endpoint
**Status**: ✅ Completed

**Step 0: Requirements**
- REST API: `POST /api/v1/units`
- Request body: `{ unitId, unitType, status }`
- Response: `201 Created` with `SuccessResponse<UnitResponseDto>` containing `{ unitId }`
- Produces event: `CreateUnitRequested` to Kafka topic `unit-events` and NATS JetStream subject `commands.unit.create`
- Validation: unitId required, unitType enum (Single, Team, Squad), status enum (Available, Assigned, In-Use, Maintenance, Out-of-Service)
- Test criteria: Verify `CreateUnitRequested` event appears in both Kafka and NATS/JetStream with correct data

**Test Criteria**:
- `testCreateUnit_WithValidData_ProducesEvent()` - Call POST /api/v1/units, verify event in both Kafka and NATS/JetStream
- `testCreateUnit_WithMissingUnitId_Returns400()` - Validation error, no event in either bus
- `testCreateUnit_WithInvalidUnitType_Returns400()` - Invalid unitType enum, no event in either bus
- `testCreateUnit_WithInvalidStatus_Returns400()` - Invalid status enum, no event in either bus
- `testCreateUnit_WithEmptyUnitId_Returns400()` - Empty unitId validation, no event in either bus
- Event contains all unit data (unitId, unitType, status)
- Event has eventId, timestamp, and aggregateId (unitId)
- Event appears in Kafka topic `unit-events`
- Event appears in NATS JetStream subject `commands.unit.create` (critical event)

**Demo Suggestion**:
1. Show POST /api/v1/units request with curl or Postman
   ```bash
   curl -X POST http://localhost:8080/api/v1/units \
     -H "Content-Type: application/json" \
     -d '{"unitId":"UNIT-001","unitType":"Single","status":"Available"}'
   ```
2. Show 201 Created response
3. Show CreateUnitRequested event in Kafka topic using kafka-console-consumer
   ```bash
   kafka-console-consumer --bootstrap-server localhost:9092 --topic unit-events --from-beginning
   ```
4. Show CreateUnitRequested event in NATS JetStream subject
   ```bash
   nats stream view commands.unit.create
   ```
5. Highlight event structure (eventId, timestamp, aggregateId, event data) in both buses
6. Show validation error example (missing unitId) - 400 Bad Request, no events published
7. Show unit types (Single, Team, Squad) and statuses (Available, Assigned, In-Use, Maintenance, Out-of-Service)
8. Explain double-publish pattern: Kafka for event sourcing, NATS/JetStream for near realtime processing

**Implementation Details**:
- Created domain enums: `UnitType` (Single, Team, Squad) and `UnitStatus` (Available, Assigned, InUse, Maintenance, OutOfService) in `edge/src/main/java/com/knowit/policesystem/edge/domain/`
- Created DTOs: `CreateUnitRequestDto` and `UnitResponseDto` with validation annotations (`@NotBlank`, `@NotNull`)
- Created `CreateUnitCommand` extending base `Command` class in `edge/src/main/java/com/knowit/policesystem/edge/commands/units/`
- Created `CreateUnitRequested` event extending base `Event` class in `common/src/main/java/com/knowit/policesystem/common/events/units/`
- Created `CreateUnitCommandValidator` with validation for required fields (unitId, unitType enum, status enum)
- Created `CreateUnitCommandHandler` that publishes events to Kafka topic "unit-events"
- Created `UnitController` with POST `/api/v1/units` endpoint extending `BaseRestController`
- DualEventPublisher automatically publishes critical events (ending in "Requested") to both Kafka and NATS/JetStream
- All components follow event-driven architecture pattern: REST Controller → Command → Command Handler → Event Publisher → Kafka Topic (and NATS/JetStream)
- Event uses request-based naming: `CreateUnitRequested` (not `UnitCreated`)
- Validation occurs at both DTO level (via `@Valid`) and command level (via `CommandValidator`)
- Created comprehensive integration tests in `UnitControllerTest` with Kafka test containers (5 test cases, all passing)
- Tests verify Kafka event production with proper event structure and metadata
- Note: NATS is disabled in test profile, but DualEventPublisher infrastructure is in place for production use

---

#### Increment 4.2: Update Unit Endpoint
**Status**: ✅ Completed

**Step 0: Requirements**
- REST API: `PUT /api/v1/units/{unitId}`
- Request body: `{ unitType, status }` (all optional)
- Response: `200 OK` with `SuccessResponse<UnitResponseDto>`
- Produces event: `UpdateUnitRequested` to Kafka topic `unit-events` and NATS JetStream subject `commands.unit.update`
- Validation: unitId required (from path), unitType enum if provided, status enum if provided
- Test criteria: Verify `UpdateUnitRequested` event appears in both Kafka and NATS/JetStream with correct data

**Test Criteria**:
- `testUpdateUnit_WithValidData_ProducesEvent()` - Call PUT /api/v1/units/{unitId}, verify event in Kafka
- `testUpdateUnit_WithAllFields_ProducesEvent()` - All fields provided, verify all fields in event
- `testUpdateUnit_WithOnlyUnitType_ProducesEvent()` - Partial update (only unitType), verify nulls for other fields
- `testUpdateUnit_WithOnlyStatus_ProducesEvent()` - Partial update (only status), verify nulls for other fields
- `testUpdateUnit_WithInvalidUnitType_Returns400()` - Invalid unitType enum, verify 400 and no event
- `testUpdateUnit_WithInvalidStatus_Returns400()` - Invalid status enum, verify 400 and no event
- `testUpdateUnit_WithEmptyUnitId_Returns400()` - Empty unitId in path, verify error
- Event contains unitId and provided fields (nulls for omitted fields)
- Event has eventId, timestamp, aggregateId
- Event appears in Kafka topic `unit-events`
- Event appears in NATS JetStream subject `commands.unit.update` (critical event)

**Implementation Details**:
- Created `UpdateUnitRequestDto` with optional fields (unitType, status) matching OpenAPI spec
- Created `UpdateUnitCommand` extending base `Command` class in `edge/src/main/java/com/knowit/policesystem/edge/commands/units/`
- Created `UpdateUnitRequested` event extending base `Event` class in `common/src/main/java/com/knowit/policesystem/common/events/units/`
- Created `UpdateUnitCommandValidator` with validation for unitId (required)
- Created `UpdateUnitCommandHandler` that publishes events to Kafka topic "unit-events"
- Added PUT endpoint to `UnitController` with path `/api/v1/units/{unitId}`
- DualEventPublisher automatically publishes critical events (ending in "Requested") to both Kafka and NATS/JetStream
- All components follow event-driven architecture pattern: REST Controller → Command → Command Handler → Event Publisher → Kafka Topic (and NATS/JetStream)
- Event uses request-based naming: `UpdateUnitRequested` (not `UnitUpdated`)
- Validation occurs at both DTO level (via `@Valid`) and command level (via `CommandValidator`)
- Created comprehensive integration tests in `UnitControllerTest` with Kafka test containers (7 new test cases, all passing)
- Tests verify Kafka event production with proper event structure and metadata
- Partial updates supported - event contains only provided fields, nulls for omitted fields
- Note: NATS is disabled in test profile, but DualEventPublisher infrastructure is in place for production use

**Demo Suggestion**:
1. Show PUT /api/v1/units/UNIT-001 request with curl or Postman
   ```bash
   curl -X PUT http://localhost:8080/api/v1/units/UNIT-001 \
     -H "Content-Type: application/json" \
     -d '{"unitType":"Team","status":"Assigned"}'
   ```
2. Show 200 OK response
3. Show UpdateUnitRequested event in Kafka topic using kafka-console-consumer
   ```bash
   kafka-console-consumer --bootstrap-server localhost:9092 --topic unit-events --from-beginning
   ```
4. Show UpdateUnitRequested event in NATS JetStream subject
   ```bash
   nats stream view commands.unit.update
   ```
5. Highlight event structure (eventId, timestamp, aggregateId, event data) in both buses
6. Show partial update example (only unitType provided) - verify other fields are null in event
7. Show validation error example (invalid unitType enum value) - 400 Bad Request, no events published
8. Show validation error example (invalid status enum value) - 400 Bad Request, no events published
9. Explain double-publish pattern: Kafka for event sourcing, NATS/JetStream for near realtime processing

---

#### Increment 4.3: Change Unit Status Endpoint
**Status**: ✅ Completed

**Step 0: Requirements**
- REST API: `PATCH /api/v1/units/{unitId}` (note: path follows OpenAPI spec, not `/status` sub-path)
- Request body: `{ status }`
- Response: `200 OK` with `SuccessResponse<UnitStatusResponseDto>` containing `{ unitId, status }`
- Produces event: `ChangeUnitStatusRequested` to Kafka topic `unit-events` and NATS JetStream subject `commands.unit.change-status`
- Validation: unitId required, status must be valid UnitStatus enum (Available, Assigned, InUse, Maintenance, OutOfService)
- Test criteria: Verify `ChangeUnitStatusRequested` event appears in both Kafka and NATS/JetStream

**Test Criteria**:
- `testChangeUnitStatus_WithValidStatus_ProducesEvent()` - Call PATCH endpoint, verify event
- `testChangeUnitStatus_WithInvalidStatus_Returns400()` - Invalid status enum, no event
- `testChangeUnitStatus_WithMissingStatus_Returns400()` - Missing status field, no event
- `testChangeUnitStatus_WithEmptyUnitId_Returns400()` - Empty unitId in path, no event
- Event contains unitId and new status
- Event has eventId, timestamp, aggregateId
- Event appears in Kafka topic `unit-events`
- Event appears in NATS JetStream subject `commands.unit.change-status` (critical event)

**Implementation Details**:
- Created DTOs: `ChangeUnitStatusRequestDto` with `@NotNull` status field and `UnitStatusResponseDto` with unitId and status fields
- Created `ChangeUnitStatusCommand` extending base `Command` class in `edge/src/main/java/com/knowit/policesystem/edge/commands/units/`
- Created `ChangeUnitStatusRequested` event extending base `Event` class in `common/src/main/java/com/knowit/policesystem/common/events/units/`
- Created `ChangeUnitStatusCommandValidator` with validation for unitId (required) and status (required, valid UnitStatus enum: Available, Assigned, InUse, Maintenance, OutOfService)
- Created `ChangeUnitStatusCommandHandler` that publishes events to Kafka topic "unit-events"
- Added PATCH endpoint to `UnitController` with path `/api/v1/units/{unitId}` (following OpenAPI spec, not `/status` sub-path)
- DualEventPublisher automatically publishes critical events (ending in "Requested") to both Kafka and NATS/JetStream
- All components follow event-driven architecture pattern: REST Controller → Command → Command Handler → Event Publisher → Kafka Topic (and NATS/JetStream)
- Event uses request-based naming: `ChangeUnitStatusRequested` (not `UnitStatusChanged`)
- Validation occurs at both DTO level (via `@Valid` and `@NotNull`) and command level (via `CommandValidator`)
- Created comprehensive integration tests in `UnitControllerTest` with Kafka test containers (4 new test cases, all passing)
- Tests verify Kafka event production with proper event structure and metadata
- Note: NATS is disabled in test profile, but DualEventPublisher infrastructure is in place for production use

**Demo Suggestion**:
1. Show PATCH /api/v1/units/UNIT-001 request with curl or Postman
   ```bash
   curl -X PATCH http://localhost:8080/api/v1/units/UNIT-001 \
     -H "Content-Type: application/json" \
     -d '{"status":"InUse"}'
   ```
2. Show 200 OK response with unitId and status
3. Show ChangeUnitStatusRequested event in Kafka topic using kafka-console-consumer
   ```bash
   kafka-console-consumer --bootstrap-server localhost:9092 --topic unit-events --from-beginning
   ```
4. Show ChangeUnitStatusRequested event in NATS JetStream subject
   ```bash
   nats stream view commands.unit.change-status
   ```
5. Highlight event structure (eventId, timestamp, aggregateId, event data) in both buses
6. Show validation error example (invalid status enum value) - 400 Bad Request, no events published
7. Show validation error example (missing status field) - 400 Bad Request, no events published
8. Explain status enum values (Available, Assigned, InUse, Maintenance, OutOfService)
9. Explain double-publish pattern: Kafka for event sourcing, NATS/JetStream for near realtime processing

---

### Phase 5: Person Domain

#### Increment 5.1: Register Person Endpoint
**Status**: ✅ Completed

**Step 0: Requirements**
- REST API: `POST /api/v1/persons`
- Request body: `{ personId, firstName, lastName, dateOfBirth, gender, race, phoneNumber }`
- Response: `201 Created` with `SuccessResponse<PersonResponseDto>` containing `{ personId }`
- Produces event: `RegisterPersonRequested` to Kafka topic `person-events` and NATS JetStream subject `commands.person.register`
- Validation: personId required, firstName required, lastName required, dateOfBirth required, gender/race enums optional
- Test criteria: Verify `RegisterPersonRequested` event appears in both Kafka and NATS/JetStream with correct data

**Test Criteria**:
- `testRegisterPerson_WithValidData_ProducesEvent()` - Call POST /api/v1/persons, verify event in both Kafka and NATS/JetStream
- `testRegisterPerson_WithAllFields_ProducesEvent()` - All fields including optional, verify all fields in event
- `testRegisterPerson_WithMissingPersonId_Returns400()` - Validation error, no event in either bus
- `testRegisterPerson_WithMissingFirstName_Returns400()` - Validation error, no event published
- `testRegisterPerson_WithMissingLastName_Returns400()` - Validation error, no event published
- `testRegisterPerson_WithMissingDateOfBirth_Returns400()` - Validation error, no event published
- `testRegisterPerson_WithInvalidDateOfBirth_Returns400()` - Date format validation, no event published
- `testRegisterPerson_WithInvalidGender_Returns400()` - Gender enum validation, no event published
- `testRegisterPerson_WithInvalidRace_Returns400()` - Race enum validation, no event published
- `testRegisterPerson_WithEmptyPersonId_Returns400()` - Empty personId validation, no event published
- Event contains all person data (personId, firstName, lastName, dateOfBirth, gender, race, phoneNumber)
- Event has eventId, timestamp, and aggregateId (personId)
- Event appears in Kafka topic `person-events`
- Event appears in NATS JetStream subject `commands.person.register` (critical event)

**Implementation Details**:
- Created domain enums: `Gender` (Male, Female, Other, Unknown) and `Race` (White, Black, Hispanic, Asian, Native, Other, Unknown) in `edge/src/main/java/com/knowit/policesystem/edge/domain/`
- Created DTOs: `RegisterPersonRequestDto` and `PersonResponseDto` with validation annotations (`@NotBlank`, `@NotNull`)
- Created `RegisterPersonCommand` extending base `Command` class in `edge/src/main/java/com/knowit/policesystem/edge/commands/persons/`
- Created `RegisterPersonRequested` event extending base `Event` class in `common/src/main/java/com/knowit/policesystem/common/events/persons/`
- Created `RegisterPersonCommandValidator` with validation for required fields (personId, firstName, lastName, dateOfBirth)
- Created `RegisterPersonCommandHandler` that publishes events to Kafka topic "person-events"
- Created `PersonController` with POST `/api/v1/persons` endpoint extending `BaseRestController`
- DualEventPublisher automatically publishes critical events (ending in "Requested") to both Kafka and NATS/JetStream
- All components follow event-driven architecture pattern: REST Controller → Command → Command Handler → Event Publisher → Kafka Topic (and NATS/JetStream)
- Event uses request-based naming: `RegisterPersonRequested` (not `PersonRegistered`)
- Validation occurs at both DTO level (via `@Valid`) and command level (via `CommandValidator`)
- Created comprehensive integration tests in `PersonControllerTest` with Kafka test containers (10 test cases, all passing)
- Tests verify Kafka event production with proper event structure and metadata
- Note: NATS is disabled in test profile, but DualEventPublisher infrastructure is in place for production use

**Demo Suggestion**:
1. Show POST /api/v1/persons request with curl or Postman
   ```bash
   curl -X POST http://localhost:8080/api/v1/persons \
     -H "Content-Type: application/json" \
     -d '{"personId":"PERSON-001","firstName":"Jane","lastName":"Smith","dateOfBirth":"1990-05-20","gender":"Female","race":"White","phoneNumber":"555-0200"}'
   ```
2. Show 201 Created response
3. Show RegisterPersonRequested event in Kafka topic using kafka-console-consumer
   ```bash
   kafka-console-consumer --bootstrap-server localhost:9092 --topic person-events --from-beginning
   ```
4. Show RegisterPersonRequested event in NATS JetStream subject
   ```bash
   nats stream view commands.person.register
   ```
5. Highlight event structure (eventId, timestamp, aggregateId, event data) in both buses
6. Show validation error example (missing personId) - 400 Bad Request, no events published
7. Show gender and race enum values
8. Explain double-publish pattern: Kafka for event sourcing, NATS/JetStream for near realtime processing

---

#### Increment 5.2: Update Person Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `PUT /api/persons/{personId}`
- Request body: `{ firstName, lastName, dateOfBirth, gender, race, phoneNumber }` (all optional)
- Response: `200 OK`
- Produces event: `UpdatePersonRequested` to Kafka topic `person-events`
- Test criteria: Verify `UpdatePersonRequested` event appears in Kafka

**Test Criteria**:
- `testUpdatePerson_WithValidData_ProducesEvent()` - Verify event
- `testUpdatePerson_WithNonExistentPersonId_Returns404()` - Not found
- Event contains personId and provided fields

**Demo Suggestion**:
1. Show PUT /api/persons/{personId} request
2. Show UpdatePersonRequested event

---

### Phase 6: Location Domain

#### Increment 6.1: Create Location Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/locations`
- Request body: `{ locationId, address, city, state, zipCode, latitude, longitude, locationType }`
- Response: `201 Created` with `{ locationId }`
- Produces event: `CreateLocationRequested` to Kafka topic `location-events`
- Validation: locationId required, coordinates format, locationType enum
- Test criteria: Verify `CreateLocationRequested` event appears in Kafka

**Test Criteria**:
- `testCreateLocation_WithValidData_ProducesEvent()` - Verify event
- `testCreateLocation_WithMissingLocationId_Returns400()` - Validation error
- `testCreateLocation_WithInvalidCoordinates_Returns400()` - Coordinate validation
- Event contains all location data

**Demo Suggestion**:
1. Show POST /api/locations request
2. Show CreateLocationRequested event in Kafka
3. Show location with coordinates

---

#### Increment 6.2: Update Location Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `PUT /api/locations/{locationId}`
- Request body: `{ address, city, state, zipCode, latitude, longitude, locationType }` (all optional)
- Response: `200 OK`
- Produces event: `UpdateLocationRequested` to Kafka topic `location-events`
- Test criteria: Verify `UpdateLocationRequested` event appears in Kafka

**Test Criteria**:
- `testUpdateLocation_WithValidData_ProducesEvent()` - Verify event
- `testUpdateLocation_WithNonExistentLocationId_Returns404()` - Not found
- Event contains locationId and provided fields

**Demo Suggestion**:
1. Show PUT /api/locations/{locationId} request
2. Show UpdateLocationRequested event

---

#### Increment 6.3: Link Location to Incident Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/incidents/{incidentId}/locations`
- Request body: `{ locationId, locationRoleType, description }`
- Response: `200 OK`
- Produces event: `LinkLocationToIncidentRequested` to Kafka topic `location-events`
- Test criteria: Verify `LinkLocationToIncidentRequested` event appears in Kafka

**Test Criteria**:
- `testLinkLocationToIncident_WithValidData_ProducesEvent()` - Verify event
- `testLinkLocationToIncident_WithNonExistentIncidentId_Returns404()` - Not found
- `testLinkLocationToIncident_WithNonExistentLocationId_Returns404()` - Not found
- Event contains incidentId, locationId, locationRoleType, description

**Demo Suggestion**:
1. Show POST /api/incidents/{incidentId}/locations request
2. Show LinkLocationToIncidentRequested event
3. Show location role types

---

#### Increment 6.4: Unlink Location from Incident Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `DELETE /api/incidents/{incidentId}/locations/{locationId}`
- Response: `200 OK`
- Produces event: `UnlinkLocationFromIncidentRequested` to Kafka topic `location-events`
- Test criteria: Verify `UnlinkLocationFromIncidentRequested` event appears in Kafka

**Test Criteria**:
- `testUnlinkLocationFromIncident_WithValidData_ProducesEvent()` - Verify event
- `testUnlinkLocationFromIncident_WithNonExistentIncidentId_Returns404()` - Not found
- Event contains incidentId and locationId

**Demo Suggestion**:
1. Show DELETE /api/incidents/{incidentId}/locations/{locationId} request
2. Show UnlinkLocationFromIncidentRequested event

---

#### Increment 6.5: Link Location to Call Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/calls/{callId}/locations`
- Request body: `{ locationId, locationRoleType, description }`
- Response: `200 OK`
- Produces event: `LinkLocationToCallRequested` to Kafka topic `location-events`
- Test criteria: Verify `LinkLocationToCallRequested` event appears in Kafka

**Test Criteria**:
- `testLinkLocationToCall_WithValidData_ProducesEvent()` - Verify event
- Event contains callId, locationId, locationRoleType, description

**Demo Suggestion**:
1. Show POST /api/calls/{callId}/locations request
2. Show LinkLocationToCallRequested event

---

#### Increment 6.6: Unlink Location from Call Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `DELETE /api/calls/{callId}/locations/{locationId}`
- Response: `200 OK`
- Produces event: `UnlinkLocationFromCallRequested` to Kafka topic `location-events`
- Test criteria: Verify `UnlinkLocationFromCallRequested` event appears in Kafka

**Test Criteria**:
- `testUnlinkLocationFromCall_WithValidData_ProducesEvent()` - Verify event
- Event contains callId and locationId

**Demo Suggestion**:
1. Show DELETE /api/calls/{callId}/locations/{locationId} request
2. Show UnlinkLocationFromCallRequested event

---

### Phase 7: Incident Domain

#### Increment 7.1: Dispatch Incident Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/incidents/{incidentId}/dispatch`
- Request body: `{ dispatchedTime }`
- Response: `200 OK`
- Produces event: `DispatchIncidentRequested` to Kafka topic `incident-events`
- Test criteria: Verify `DispatchIncidentRequested` event appears in Kafka

**Test Criteria**:
- `testDispatchIncident_WithValidData_ProducesEvent()` - Verify event
- `testDispatchIncident_WithNonExistentIncidentId_Returns404()` - Not found
- Event contains incidentId and dispatchedTime

**Demo Suggestion**:
1. Show POST /api/incidents/{incidentId}/dispatch request
2. Show DispatchIncidentRequested event
3. Show incident status transition

---

#### Increment 7.2: Arrive at Incident Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/incidents/{incidentId}/arrive`
- Request body: `{ arrivedTime }`
- Response: `200 OK`
- Produces event: `ArriveAtIncidentRequested` to Kafka topic `incident-events`
- Test criteria: Verify `ArriveAtIncidentRequested` event appears in Kafka

**Test Criteria**:
- `testArriveAtIncident_WithValidData_ProducesEvent()` - Verify event
- `testArriveAtIncident_WithNonExistentIncidentId_Returns404()` - Not found
- Event contains incidentId and arrivedTime

**Demo Suggestion**:
1. Show POST /api/incidents/{incidentId}/arrive request
2. Show ArriveAtIncidentRequested event
3. Show timeline progression (reported -> dispatched -> arrived)

---

#### Increment 7.3: Clear Incident Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/incidents/{incidentId}/clear`
- Request body: `{ clearedTime }`
- Response: `200 OK`
- Produces event: `ClearIncidentRequested` to Kafka topic `incident-events`
- Test criteria: Verify `ClearIncidentRequested` event appears in Kafka

**Test Criteria**:
- `testClearIncident_WithValidData_ProducesEvent()` - Verify event
- `testClearIncident_WithNonExistentIncidentId_Returns404()` - Not found
- Event contains incidentId and clearedTime

**Demo Suggestion**:
1. Show POST /api/incidents/{incidentId}/clear request
2. Show ClearIncidentRequested event
3. Show complete incident lifecycle

---

#### Increment 7.4: Change Incident Status Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `PATCH /api/incidents/{incidentId}/status`
- Request body: `{ status }`
- Response: `200 OK`
- Produces event: `ChangeIncidentStatusRequested` to Kafka topic `incident-events`
- Test criteria: Verify `ChangeIncidentStatusRequested` event appears in Kafka

**Test Criteria**:
- `testChangeIncidentStatus_WithValidStatus_ProducesEvent()` - Verify event
- `testChangeIncidentStatus_WithInvalidStatus_Returns400()` - Invalid status
- Event contains incidentId and status

**Demo Suggestion**:
1. Show PATCH /api/incidents/{incidentId}/status request
2. Show ChangeIncidentStatusRequested event
3. Show status transitions

---

#### Increment 7.5: Update Incident Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `PUT /api/incidents/{incidentId}`
- Request body: `{ priority, description, incidentType }` (all optional)
- Response: `200 OK`
- Produces event: `UpdateIncidentRequested` to Kafka topic `incident-events`
- Test criteria: Verify `UpdateIncidentRequested` event appears in Kafka

**Test Criteria**:
- `testUpdateIncident_WithValidData_ProducesEvent()` - Verify event
- `testUpdateIncident_WithNonExistentIncidentId_Returns404()` - Not found
- Event contains incidentId and provided fields

**Demo Suggestion**:
1. Show PUT /api/incidents/{incidentId} request
2. Show UpdateIncidentRequested event
3. Show updating incident description

---

### Phase 8: CallForService Domain

#### Increment 8.1: Receive Call Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/calls`
- Request body: `{ callId, callNumber, priority, status, receivedTime, description, callType }`
- Response: `201 Created` with `{ callId, callNumber }`
- Produces event: `ReceiveCallRequested` to Kafka topic `call-events`
- Validation: callId required, priority enum, status enum, callType enum
- Test criteria: Verify `ReceiveCallRequested` event appears in Kafka

**Test Criteria**:
- `testReceiveCall_WithValidData_ProducesEvent()` - Verify event
- `testReceiveCall_WithMissingCallId_Returns400()` - Validation error
- `testReceiveCall_WithInvalidPriority_Returns400()` - Priority validation
- Event contains all call data

**Demo Suggestion**:
1. Show POST /api/calls request
2. Show ReceiveCallRequested event in Kafka
3. Show call types and priorities

---

#### Increment 8.2: Dispatch Call Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/calls/{callId}/dispatch`
- Request body: `{ dispatchedTime }`
- Response: `200 OK`
- Produces event: `DispatchCallRequested` to Kafka topic `call-events`
- Test criteria: Verify `DispatchCallRequested` event appears in Kafka

**Test Criteria**:
- `testDispatchCall_WithValidData_ProducesEvent()` - Verify event
- `testDispatchCall_WithNonExistentCallId_Returns404()` - Not found
- Event contains callId and dispatchedTime

**Demo Suggestion**:
1. Show POST /api/calls/{callId}/dispatch request
2. Show DispatchCallRequested event

---

#### Increment 8.3: Arrive at Call Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/calls/{callId}/arrive`
- Request body: `{ arrivedTime }`
- Response: `200 OK`
- Produces event: `ArriveAtCallRequested` to Kafka topic `call-events`
- Test criteria: Verify `ArriveAtCallRequested` event appears in Kafka

**Test Criteria**:
- `testArriveAtCall_WithValidData_ProducesEvent()` - Verify event
- Event contains callId and arrivedTime

**Demo Suggestion**:
1. Show POST /api/calls/{callId}/arrive request
2. Show ArriveAtCallRequested event

---

#### Increment 8.4: Clear Call Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/calls/{callId}/clear`
- Request body: `{ clearedTime }`
- Response: `200 OK`
- Produces event: `ClearCallRequested` to Kafka topic `call-events`
- Test criteria: Verify `ClearCallRequested` event appears in Kafka

**Test Criteria**:
- `testClearCall_WithValidData_ProducesEvent()` - Verify event
- Event contains callId and clearedTime

**Demo Suggestion**:
1. Show POST /api/calls/{callId}/clear request
2. Show ClearCallRequested event

---

#### Increment 8.5: Change Call Status Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `PATCH /api/calls/{callId}/status`
- Request body: `{ status }`
- Response: `200 OK`
- Produces event: `ChangeCallStatusRequested` to Kafka topic `call-events`
- Test criteria: Verify `ChangeCallStatusRequested` event appears in Kafka

**Test Criteria**:
- `testChangeCallStatus_WithValidStatus_ProducesEvent()` - Verify event
- Event contains callId and status

**Demo Suggestion**:
1. Show PATCH /api/calls/{callId}/status request
2. Show ChangeCallStatusRequested event

---

#### Increment 8.6: Update Call Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `PUT /api/calls/{callId}`
- Request body: `{ priority, description, callType }` (all optional)
- Response: `200 OK`
- Produces event: `UpdateCallRequested` to Kafka topic `call-events`
- Test criteria: Verify `UpdateCallRequested` event appears in Kafka

**Test Criteria**:
- `testUpdateCall_WithValidData_ProducesEvent()` - Verify event
- Event contains callId and provided fields

**Demo Suggestion**:
1. Show PUT /api/calls/{callId} request
2. Show UpdateCallRequested event

---

#### Increment 8.7: Link Call to Incident Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/calls/{callId}/incidents`
- Request body: `{ incidentId }`
- Response: `200 OK`
- Produces event: `LinkCallToIncidentRequested` to Kafka topic `call-events`
- Test criteria: Verify `LinkCallToIncidentRequested` event appears in Kafka

**Test Criteria**:
- `testLinkCallToIncident_WithValidData_ProducesEvent()` - Verify event
- Event contains callId and incidentId

**Demo Suggestion**:
1. Show POST /api/calls/{callId}/incidents request
2. Show LinkCallToIncidentRequested event
3. Show relationship between calls and incidents

---

#### Increment 8.8: Link Call to Dispatch Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/calls/{callId}/dispatches`
- Request body: `{ dispatchId }`
- Response: `200 OK`
- Produces event: `LinkCallToDispatchRequested` to Kafka topic `call-events`
- Test criteria: Verify `LinkCallToDispatchRequested` event appears in Kafka

**Test Criteria**:
- `testLinkCallToDispatch_WithValidData_ProducesEvent()` - Verify event
- Event contains callId and dispatchId

**Demo Suggestion**:
1. Show POST /api/calls/{callId}/dispatches request
2. Show LinkCallToDispatchRequested event

---

### Phase 9: Activity Domain

#### Increment 9.1: Start Activity Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/activities`
- Request body: `{ activityId, activityTime, activityType, description, status }`
- Response: `201 Created` with `{ activityId }`
- Produces event: `StartActivityRequested` to Kafka topic `activity-events`
- Validation: activityId required, activityType enum, status enum
- Test criteria: Verify `StartActivityRequested` event appears in Kafka

**Test Criteria**:
- `testStartActivity_WithValidData_ProducesEvent()` - Verify event
- `testStartActivity_WithMissingActivityId_Returns400()` - Validation error
- Event contains all activity data

**Demo Suggestion**:
1. Show POST /api/activities request
2. Show StartActivityRequested event in Kafka
3. Show activity types

---

#### Increment 9.2: Complete Activity Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/activities/{activityId}/complete`
- Request body: `{ completedTime }`
- Response: `200 OK`
- Produces event: `CompleteActivityRequested` to Kafka topic `activity-events`
- Test criteria: Verify `CompleteActivityRequested` event appears in Kafka

**Test Criteria**:
- `testCompleteActivity_WithValidData_ProducesEvent()` - Verify event
- Event contains activityId and completedTime

**Demo Suggestion**:
1. Show POST /api/activities/{activityId}/complete request
2. Show CompleteActivityRequested event

---

#### Increment 9.3: Change Activity Status Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `PATCH /api/activities/{activityId}/status`
- Request body: `{ status }`
- Response: `200 OK`
- Produces event: `ChangeActivityStatusRequested` to Kafka topic `activity-events`
- Test criteria: Verify `ChangeActivityStatusRequested` event appears in Kafka

**Test Criteria**:
- `testChangeActivityStatus_WithValidStatus_ProducesEvent()` - Verify event
- Event contains activityId and status

**Demo Suggestion**:
1. Show PATCH /api/activities/{activityId}/status request
2. Show ChangeActivityStatusRequested event

---

#### Increment 9.4: Update Activity Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `PUT /api/activities/{activityId}`
- Request body: `{ description }` (optional)
- Response: `200 OK`
- Produces event: `UpdateActivityRequested` to Kafka topic `activity-events`
- Test criteria: Verify `UpdateActivityRequested` event appears in Kafka

**Test Criteria**:
- `testUpdateActivity_WithValidData_ProducesEvent()` - Verify event
- Event contains activityId and description

**Demo Suggestion**:
1. Show PUT /api/activities/{activityId} request
2. Show UpdateActivityRequested event

---

#### Increment 9.5: Link Activity to Incident Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/activities/{activityId}/incidents`
- Request body: `{ incidentId }`
- Response: `200 OK`
- Produces event: `LinkActivityToIncidentRequested` to Kafka topic `activity-events`
- Test criteria: Verify `LinkActivityToIncidentRequested` event appears in Kafka

**Test Criteria**:
- `testLinkActivityToIncident_WithValidData_ProducesEvent()` - Verify event
- Event contains activityId and incidentId

**Demo Suggestion**:
1. Show POST /api/activities/{activityId}/incidents request
2. Show LinkActivityToIncidentRequested event

---

### Phase 10: Assignment Domain

#### Increment 10.1: Create Assignment Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/assignments`
- Request body: `{ assignmentId, assignedTime, assignmentType, status, incidentId, callId }` (incidentId XOR callId)
- Response: `201 Created` with `{ assignmentId }`
- Produces event: `CreateAssignmentRequested` to Kafka topic `assignment-events`
- Validation: assignmentId required, assignmentType enum, status enum, exactly one of incidentId or callId
- Test criteria: Verify `CreateAssignmentRequested` event appears in Kafka

**Test Criteria**:
- `testCreateAssignment_WithIncidentId_ProducesEvent()` - Verify event with incidentId
- `testCreateAssignment_WithCallId_ProducesEvent()` - Verify event with callId
- `testCreateAssignment_WithBothIncidentAndCall_Returns400()` - Validation error
- `testCreateAssignment_WithNeitherIncidentNorCall_Returns400()` - Validation error
- Event contains assignmentId, assignedTime, assignmentType, status, and incidentId or callId

**Demo Suggestion**:
1. Show POST /api/assignments request with incidentId
2. Show CreateAssignmentRequested event
3. Show assignment with callId
4. Show validation error (both incidentId and callId)

---

#### Increment 10.2: Complete Assignment Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/assignments/{assignmentId}/complete`
- Request body: `{ completedTime }`
- Response: `200 OK`
- Produces event: `CompleteAssignmentRequested` to Kafka topic `assignment-events`
- Test criteria: Verify `CompleteAssignmentRequested` event appears in Kafka

**Test Criteria**:
- `testCompleteAssignment_WithValidData_ProducesEvent()` - Verify event
- Event contains assignmentId and completedTime

**Demo Suggestion**:
1. Show POST /api/assignments/{assignmentId}/complete request
2. Show CompleteAssignmentRequested event

---

#### Increment 10.3: Change Assignment Status Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `PATCH /api/assignments/{assignmentId}/status`
- Request body: `{ status }`
- Response: `200 OK`
- Produces event: `ChangeAssignmentStatusRequested` to Kafka topic `assignment-events`
- Test criteria: Verify `ChangeAssignmentStatusRequested` event appears in Kafka

**Test Criteria**:
- `testChangeAssignmentStatus_WithValidStatus_ProducesEvent()` - Verify event
- Event contains assignmentId and status

**Demo Suggestion**:
1. Show PATCH /api/assignments/{assignmentId}/status request
2. Show ChangeAssignmentStatusRequested event

---

#### Increment 10.4: Link Assignment to Dispatch Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/assignments/{assignmentId}/dispatches`
- Request body: `{ dispatchId }`
- Response: `200 OK`
- Produces event: `LinkAssignmentToDispatchRequested` to Kafka topic `assignment-events`
- Test criteria: Verify `LinkAssignmentToDispatchRequested` event appears in Kafka

**Test Criteria**:
- `testLinkAssignmentToDispatch_WithValidData_ProducesEvent()` - Verify event
- Event contains assignmentId and dispatchId

**Demo Suggestion**:
1. Show POST /api/assignments/{assignmentId}/dispatches request
2. Show LinkAssignmentToDispatchRequested event

---

### Phase 11: Shift Domain

#### Increment 11.1: Start Shift Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/shifts`
- Request body: `{ shiftId, startTime, endTime, shiftType, status }`
- Response: `201 Created` with `{ shiftId }`
- Produces event: `StartShiftRequested` to Kafka topic `shift-events`
- Validation: shiftId required, shiftType enum, status enum
- Test criteria: Verify `StartShiftRequested` event appears in Kafka

**Test Criteria**:
- `testStartShift_WithValidData_ProducesEvent()` - Verify event
- `testStartShift_WithMissingShiftId_Returns400()` - Validation error
- Event contains all shift data

**Demo Suggestion**:
1. Show POST /api/shifts request
2. Show StartShiftRequested event in Kafka
3. Show shift types

---

#### Increment 11.2: End Shift Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/shifts/{shiftId}/end`
- Request body: `{ endTime }`
- Response: `200 OK`
- Produces event: `EndShiftRequested` to Kafka topic `shift-events`
- Test criteria: Verify `EndShiftRequested` event appears in Kafka

**Test Criteria**:
- `testEndShift_WithValidData_ProducesEvent()` - Verify event
- Event contains shiftId and endTime

**Demo Suggestion**:
1. Show POST /api/shifts/{shiftId}/end request
2. Show EndShiftRequested event

---

#### Increment 11.3: Change Shift Status Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `PATCH /api/shifts/{shiftId}/status`
- Request body: `{ status }`
- Response: `200 OK`
- Produces event: `ChangeShiftStatusRequested` to Kafka topic `shift-events`
- Test criteria: Verify `ChangeShiftStatusRequested` event appears in Kafka

**Test Criteria**:
- `testChangeShiftStatus_WithValidStatus_ProducesEvent()` - Verify event
- Event contains shiftId and status

**Demo Suggestion**:
1. Show PATCH /api/shifts/{shiftId}/status request
2. Show ChangeShiftStatusRequested event

---

#### Increment 11.4: Record Shift Change Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/shifts/{shiftId}/shift-changes`
- Request body: `{ shiftChangeId, changeTime, changeType, notes }`
- Response: `201 Created` with `{ shiftChangeId }`
- Produces event: `RecordShiftChangeRequested` to Kafka topic `shift-events`
- Validation: shiftChangeId required, changeType enum
- Test criteria: Verify `RecordShiftChangeRequested` event appears in Kafka

**Test Criteria**:
- `testRecordShiftChange_WithValidData_ProducesEvent()` - Verify event
- Event contains shiftId, shiftChangeId, changeTime, changeType, notes

**Demo Suggestion**:
1. Show POST /api/shifts/{shiftId}/shift-changes request
2. Show RecordShiftChangeRequested event
3. Show shift change types

---

### Phase 12: Dispatch Domain

#### Increment 12.1: Create Dispatch Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/dispatches`
- Request body: `{ dispatchId, dispatchTime, dispatchType, status }`
- Response: `201 Created` with `{ dispatchId }`
- Produces event: `CreateDispatchRequested` to Kafka topic `dispatch-events`
- Validation: dispatchId required, dispatchType enum, status enum
- Test criteria: Verify `CreateDispatchRequested` event appears in Kafka

**Test Criteria**:
- `testCreateDispatch_WithValidData_ProducesEvent()` - Verify event
- `testCreateDispatch_WithMissingDispatchId_Returns400()` - Validation error
- Event contains all dispatch data

**Demo Suggestion**:
1. Show POST /api/dispatches request
2. Show CreateDispatchRequested event in Kafka
3. Show dispatch types

---

#### Increment 12.2: Change Dispatch Status Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `PATCH /api/dispatches/{dispatchId}/status`
- Request body: `{ status }`
- Response: `200 OK`
- Produces event: `ChangeDispatchStatusRequested` to Kafka topic `dispatch-events`
- Test criteria: Verify `ChangeDispatchStatusRequested` event appears in Kafka

**Test Criteria**:
- `testChangeDispatchStatus_WithValidStatus_ProducesEvent()` - Verify event
- Event contains dispatchId and status

**Demo Suggestion**:
1. Show PATCH /api/dispatches/{dispatchId}/status request
2. Show ChangeDispatchStatusRequested event

---

### Phase 13: ResourceAssignment Domain

#### Increment 13.1: Assign Resource Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/assignments/{assignmentId}/resources`
- Request body: `{ resourceId, resourceType, roleType, status, startTime }`
- Response: `201 Created` with `{ resourceAssignmentId }`
- Produces event: `AssignResourceRequested` to Kafka topic `resource-assignment-events`
- Validation: resourceId required, resourceType enum (Officer, Vehicle, Unit), roleType enum, status enum
- Test criteria: Verify `AssignResourceRequested` event appears in Kafka

**Test Criteria**:
- `testAssignResource_WithOfficer_ProducesEvent()` - Verify event with Officer resource
- `testAssignResource_WithVehicle_ProducesEvent()` - Verify event with Vehicle resource
- `testAssignResource_WithUnit_ProducesEvent()` - Verify event with Unit resource
- `testAssignResource_WithInvalidResourceType_Returns400()` - Validation error
- Event contains assignmentId, resourceId, resourceType, roleType, status, startTime

**Demo Suggestion**:
1. Show POST /api/assignments/{assignmentId}/resources request
2. Show AssignResourceRequested event
3. Show different resource types (Officer, Vehicle, Unit)
4. Show role types

---

#### Increment 13.2: Unassign Resource Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `DELETE /api/assignments/{assignmentId}/resources/{resourceId}`
- Request body: `{ endTime }`
- Response: `200 OK`
- Produces event: `UnassignResourceRequested` to Kafka topic `resource-assignment-events`
- Test criteria: Verify `UnassignResourceRequested` event appears in Kafka

**Test Criteria**:
- `testUnassignResource_WithValidData_ProducesEvent()` - Verify event
- Event contains assignmentId, resourceId, endTime

**Demo Suggestion**:
1. Show DELETE /api/assignments/{assignmentId}/resources/{resourceId} request
2. Show UnassignResourceRequested event

---

#### Increment 13.3: Change Resource Assignment Status Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `PATCH /api/assignments/{assignmentId}/resources/{resourceId}/status`
- Request body: `{ status }`
- Response: `200 OK`
- Produces event: `ChangeResourceAssignmentStatusRequested` to Kafka topic `resource-assignment-events`
- Test criteria: Verify `ChangeResourceAssignmentStatusRequested` event appears in Kafka

**Test Criteria**:
- `testChangeResourceAssignmentStatus_WithValidStatus_ProducesEvent()` - Verify event
- Event contains assignmentId, resourceId, status

**Demo Suggestion**:
1. Show PATCH /api/assignments/{assignmentId}/resources/{resourceId}/status request
2. Show ChangeResourceAssignmentStatusRequested event

---

### Phase 14: InvolvedParty Domain

#### Increment 14.1: Involve Party Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/involved-parties`
- Request body: `{ personId, incidentId, callId, activityId, partyRoleType, description, involvementStartTime }` (exactly one of incidentId, callId, or activityId)
- Response: `201 Created` with `{ involvementId }`
- Produces event: `InvolvePartyRequested` to Kafka topic `involved-party-events`
- Validation: personId required, partyRoleType enum, exactly one of incidentId, callId, or activityId
- Test criteria: Verify `InvolvePartyRequested` event appears in Kafka

**Test Criteria**:
- `testInvolveParty_WithIncident_ProducesEvent()` - Verify event with incidentId
- `testInvolveParty_WithCall_ProducesEvent()` - Verify event with callId
- `testInvolveParty_WithActivity_ProducesEvent()` - Verify event with activityId
- `testInvolveParty_WithMultipleTargets_Returns400()` - Validation error
- Event contains personId, incidentId/callId/activityId, partyRoleType, description, involvementStartTime

**Demo Suggestion**:
1. Show POST /api/involved-parties request with incidentId
2. Show InvolvePartyRequested event
3. Show party role types (Victim, Suspect, Witness, etc.)
4. Show involvement with call and activity

---

#### Increment 14.2: End Party Involvement Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/involved-parties/{involvementId}/end`
- Request body: `{ involvementEndTime }`
- Response: `200 OK`
- Produces event: `EndPartyInvolvementRequested` to Kafka topic `involved-party-events`
- Test criteria: Verify `EndPartyInvolvementRequested` event appears in Kafka

**Test Criteria**:
- `testEndPartyInvolvement_WithValidData_ProducesEvent()` - Verify event
- Event contains involvementId and involvementEndTime

**Demo Suggestion**:
1. Show POST /api/involved-parties/{involvementId}/end request
2. Show EndPartyInvolvementRequested event

---

#### Increment 14.3: Update Party Involvement Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `PUT /api/involved-parties/{involvementId}`
- Request body: `{ partyRoleType, description }` (all optional)
- Response: `200 OK`
- Produces event: `UpdatePartyInvolvementRequested` to Kafka topic `involved-party-events`
- Test criteria: Verify `UpdatePartyInvolvementRequested` event appears in Kafka

**Test Criteria**:
- `testUpdatePartyInvolvement_WithValidData_ProducesEvent()` - Verify event
- Event contains involvementId and provided fields

**Demo Suggestion**:
1. Show PUT /api/involved-parties/{involvementId} request
2. Show UpdatePartyInvolvementRequested event

---

### Phase 15: OfficerShift Domain

#### Increment 15.1: Check In Officer Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/shifts/{shiftId}/officers/{badgeNumber}/check-in`
- Request body: `{ checkInTime, shiftRoleType }`
- Response: `200 OK`
- Produces event: `CheckInOfficerRequested` to Kafka topic `officer-shift-events`
- Validation: shiftRoleType enum
- Test criteria: Verify `CheckInOfficerRequested` event appears in Kafka

**Test Criteria**:
- `testCheckInOfficer_WithValidData_ProducesEvent()` - Verify event
- `testCheckInOfficer_WithInvalidShiftRoleType_Returns400()` - Validation error
- Event contains shiftId, badgeNumber, checkInTime, shiftRoleType

**Demo Suggestion**:
1. Show POST /api/shifts/{shiftId}/officers/{badgeNumber}/check-in request
2. Show CheckInOfficerRequested event
3. Show shift role types

---

#### Increment 15.2: Check Out Officer Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `POST /api/shifts/{shiftId}/officers/{badgeNumber}/check-out`
- Request body: `{ checkOutTime }`
- Response: `200 OK`
- Produces event: `CheckOutOfficerRequested` to Kafka topic `officer-shift-events`
- Test criteria: Verify `CheckOutOfficerRequested` event appears in Kafka

**Test Criteria**:
- `testCheckOutOfficer_WithValidData_ProducesEvent()` - Verify event
- Event contains shiftId, badgeNumber, checkOutTime

**Demo Suggestion**:
1. Show POST /api/shifts/{shiftId}/officers/{badgeNumber}/check-out request
2. Show CheckOutOfficerRequested event

---

#### Increment 15.3: Update Officer Shift Endpoint
**Status**: ⏳ Pending

**Step 0: Requirements**
- REST API: `PUT /api/shifts/{shiftId}/officers/{badgeNumber}`
- Request body: `{ shiftRoleType }` (optional)
- Response: `200 OK`
- Produces event: `UpdateOfficerShiftRequested` to Kafka topic `officer-shift-events`
- Test criteria: Verify `UpdateOfficerShiftRequested` event appears in Kafka

**Test Criteria**:
- `testUpdateOfficerShift_WithValidData_ProducesEvent()` - Verify event
- Event contains shiftId, badgeNumber, shiftRoleType

**Demo Suggestion**:
1. Show PUT /api/shifts/{shiftId}/officers/{badgeNumber} request
2. Show UpdateOfficerShiftRequested event

---

## Development Guidelines

### Increment Selection
- Start with Phase 1 (Foundation) before moving to other phases
- **Increment 1.5 (NATS/JetStream Infrastructure) must be completed before implementing domain features** that require dual publishing
- Within each phase, increments can be developed in parallel when dependencies allow
- Always complete infrastructure increments before dependent feature increments

### Testing Strategy
- Each increment must include comprehensive tests
- All tests follow the pattern: Call API → Verify Events (Kafka and NATS/JetStream for critical events)
- Use Kafka test containers and NATS test containers for testing
- Tests verify event structure and data correctness in both event buses
- Critical events (commands) must be verified in both Kafka and NATS/JetStream
- Non-critical events are verified in Kafka only
- No state reconstruction or projection testing in initial development

### Code Quality
- Follow Java coding standards
- Maintain test coverage above 80%
- Use meaningful names and documentation
- Keep methods and classes focused and small
- Refactor continuously

### Documentation
- Update architecture documentation as system evolves
- Document API changes
- Keep domain model documentation current
- Maintain development plan status

## Status Legend

- ⏳ Pending - Not started
- 🔄 In Progress - Currently being developed
- ✅ Completed - Feature complete and tested
- ⚠️ Blocked - Waiting on dependencies

## Notes

- This plan is a living document and will be updated as development progresses
- Each increment follows the strict 8-step process defined in AGENTS.md
- Dependencies between increments should be carefully managed
- Regular reviews should be conducted to ensure plan accuracy
- All events use request-based naming (e.g., `RegisterOfficerRequested` not `OfficerRegistered`)
- Focus is on event production, not state reconstruction or projections

### Double-Publish Pattern Implementation Notes

- **All command events** (events ending in `Requested`) are considered critical and must be published to both Kafka and NATS/JetStream
- **NATS JetStream subject naming**: `commands.{domain}.{action}` (e.g., `commands.officer.register`, `commands.incident.report`)
- **Kafka topic naming**: `{domain}-events` (e.g., `officer-events`, `incident-events`)
- **Test verification**: All critical events must be verified in both event buses
- **Increment 1.5** must be completed before implementing domain features that require dual publishing
- For increments not yet updated with dual-publish test criteria, they should be updated when implemented to include verification of both Kafka and NATS/JetStream events

