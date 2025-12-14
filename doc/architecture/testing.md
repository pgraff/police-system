# Testing Strategy and Principles

This document describes the testing approach, principles, and patterns used throughout the Police Incident Management System.

## Table of Contents

- [Testing Principles](#testing-principles)
- [Testing Patterns](#testing-patterns)
- [Test Infrastructure](#test-infrastructure)
- [Test Structure](#test-structure)
- [Edge Layer Testing](#edge-layer-testing)
- [Projection Layer Testing](#projection-layer-testing)
- [Unit Testing](#unit-testing)
- [Running Tests](#running-tests)

## Testing Principles

### 1. Test-Driven Development (TDD)

All features are developed using Test-Driven Development:

1. **Red Phase**: Write tests first that initially fail
2. **Green Phase**: Implement minimal code to make tests pass
3. **Refactor Phase**: Improve code while keeping tests green

This ensures:
- Tests are written before implementation
- Code is designed to be testable
- Tests serve as executable documentation
- Regression prevention through comprehensive test coverage

### 2. Event-Driven Testing

The system follows an event-driven architecture, and tests reflect this:

- **Edge Layer**: Tests verify that REST API calls produce correct events to Kafka/NATS
- **Projection Layer**: Tests verify that events result in correct projection state
- **No State Verification in Edge**: Edge layer tests don't verify state changes, only event production
- **Event Data Verification**: Tests verify event structure, content, and metadata

### 3. Integration Testing with Real Infrastructure

Tests use **Testcontainers** to run real infrastructure components:

- **Kafka**: Real Kafka brokers for event production/consumption testing
- **NATS/JetStream**: Real NATS servers for dual-publish verification
- **PostgreSQL**: Real database for projection state verification
- **Shared Containers**: Containers are shared across test classes for performance

This approach ensures:
- Tests run against real infrastructure, not mocks
- Tests catch integration issues early
- Tests are more reliable and closer to production behavior
- Faster test execution through container reuse

### 4. Comprehensive Coverage

Tests cover multiple scenarios:

- **Happy Path**: Valid inputs produce expected events/results
- **Edge Cases**: Boundary conditions, empty values, null handling
- **Error Conditions**: Invalid inputs, validation failures, missing data
- **Idempotency**: Duplicate events don't cause corruption
- **Async Behavior**: Proper handling of asynchronous operations

## Testing Patterns

### Edge Layer Testing Pattern

Edge layer tests follow this pattern:

```
1. Given: Prepare test data (request DTOs)
2. When: Call REST API endpoint via MockMvc
3. Then: 
   - Verify HTTP response (status, body)
   - Verify event produced to Kafka (topic, key, value)
   - Verify event produced to NATS (for critical events)
   - Verify event data matches request data
   - Verify no event produced on validation errors
```

**Example** (`OfficerControllerTest`):
```java
@Test
void testRegisterOfficer_WithValidData_ProducesEvent() throws Exception {
    // Given
    RegisterOfficerRequestDto request = new RegisterOfficerRequestDto(...);
    
    // When
    mockMvc.perform(post("/api/v1/officers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isCreated());
    
    // Then - verify Kafka event
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
    RegisterOfficerRequested event = deserialize(records.iterator().next().value());
    assertThat(event.getBadgeNumber()).isEqualTo(badgeNumber);
    // ... verify all event fields
    
    // Then - verify NATS event (for critical events)
    Message natsMsg = natsSubscription.nextMessage(Duration.ofSeconds(5));
    assertThat(natsMsg).isNotNull();
}
```

### Projection Layer Testing Pattern

Projection layer tests follow this pattern:

```
1. Given: Prepare event data
2. When: Publish event to Kafka (and optionally NATS)
3. Then:
   - Wait for async processing (using Awaitility)
   - Verify projection state in database (via JDBC)
   - Verify query endpoint returns correct data
   - Verify idempotency (duplicate events don't corrupt state)
```

**Example** (`OfficerProjectionIntegrationTest`):
```java
@Test
void registerEvent_shouldPersistProjectionAndExposeQuery() throws Exception {
    // Given
    RegisterOfficerRequested event = new RegisterOfficerRequested(...);
    
    // When
    publishToKafka(event.getBadgeNumber(), event);
    
    // Then - verify via query endpoint
    ResponseEntity<Map> response = awaitOfficer(badge);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().get("badgeNumber")).isEqualTo(badge);
    // ... verify all fields
}
```

### Unit Testing Pattern

Unit tests for infrastructure components follow this pattern:

```
1. Given: Prepare test objects
2. When: Execute method/operation
3. Then: Verify behavior and state
```

**Example** (`CommandTest`):
```java
@Test
void testCommand_WithDefaultConstructor_GeneratesIdAndTimestamp() {
    // Given & When
    TestCommand command = new TestCommand();
    
    // Then
    assertThat(command.getCommandId()).isNotNull();
    assertThat(command.getTimestamp()).isNotNull();
    assertThat(command.getCommandType()).isEqualTo("TestCommand");
}
```

## Test Infrastructure

### BaseIntegrationTest (Edge Layer)

The `BaseIntegrationTest` class provides shared infrastructure for edge layer tests:

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    protected static final KafkaContainer kafka = new KafkaContainer(...);
    protected static final NatsTestContainer nats = new NatsTestContainer();
    
    // Shared containers started once, reused across all tests
}
```

**Features**:
- Shared Kafka and NATS containers (singleton pattern)
- Containers started once, reused across all test classes
- Automatic cleanup via shutdown hooks
- Spring Boot test context with MockMvc support

### IntegrationTestBase (Projection Layer)

The `IntegrationTestBase` class provides shared infrastructure for projection layer tests:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {
    protected static final KafkaContainer kafka = ...;
    protected static final PostgreSQLContainer<?> postgres = ...;
    protected static final NatsTestContainer nats = ...;
}
```

**Features**:
- Shared Kafka, PostgreSQL, and NATS containers
- Spring Boot test context with TestRestTemplate
- JDBC access for direct database verification
- Random port for web server to avoid conflicts

### Test Utilities

#### Kafka Consumer Setup

Edge layer tests create Kafka consumers to verify event production:

```java
@BeforeEach
void setUp() {
    Properties consumerProps = new Properties();
    consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group-" + System.currentTimeMillis());
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    consumer = new KafkaConsumer<>(consumerProps);
    consumer.subscribe(Collections.singletonList(topicConfiguration.OFFICER_EVENTS));
}
```

**Key Points**:
- Use unique consumer group IDs to avoid conflicts
- Use "latest" offset to only receive new messages
- Subscribe before API calls to ensure no messages are missed

#### NATS Test Helper

Edge layer tests use `NatsTestHelper` for NATS verification:

```java
natsHelper = new NatsTestHelper(nats.getNatsUrl(), eventObjectMapper);
JetStreamSubscription subscription = natsHelper.prepareSubscription("commands.officer.register");
subscription.pull(1);
// ... after API call
Message natsMsg = subscription.nextMessage(Duration.ofSeconds(5));
```

#### Awaitility for Async Testing

Projection layer tests use Awaitility to handle async operations:

```java
private ResponseEntity<Map> awaitOfficer(String badge) {
    return Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofSeconds(1))
            .until(() -> restTemplate.getForEntity("/api/projections/officers/{badge}", Map.class, badge),
                    response -> response.getStatusCode().is2xxSuccessful());
}
```

## Test Structure

### Test Naming Convention

Tests use descriptive names following the pattern:

```
test{Method}_{Condition}_{ExpectedResult}
```

**Examples**:
- `testRegisterOfficer_WithValidData_ProducesEvent`
- `testRegisterOfficer_WithMissingBadgeNumber_Returns400`
- `testUpdateOfficer_WithOnlyFirstName_ProducesEvent`
- `testChangeOfficerStatus_WithInvalidStatus_Returns400`

### Given-When-Then Structure

All tests follow the Given-When-Then structure:

```java
@Test
void testMethod_Condition_Result() throws Exception {
    // Given - setup test data and conditions
    String badgeNumber = "12345";
    RegisterOfficerRequestDto request = new RegisterOfficerRequestDto(...);
    
    // When - execute the action being tested
    mockMvc.perform(post("/api/v1/officers")...)
        .andExpect(status().isCreated());
    
    // Then - verify the results
    ConsumerRecords<String, String> records = consumer.poll(...);
    assertThat(records).isNotEmpty();
    // ... additional assertions
}
```

### Test Organization

Tests are organized by layer and component:

```
edge/src/test/java/com/knowit/policesystem/edge/
├── controllers/          # Controller integration tests
│   ├── OfficerControllerTest.java
│   ├── IncidentControllerTest.java
│   └── ...
├── commands/            # Command unit tests
│   ├── CommandTest.java
│   └── CommandHandlerTest.java
├── queries/             # Query unit tests
│   ├── QueryTest.java
│   └── QueryHandlerTest.java
├── validation/          # Validation framework tests
│   └── ValidationFrameworkTest.java
├── infrastructure/      # Infrastructure tests
│   ├── BaseIntegrationTest.java
│   └── RestApiInfrastructureTest.java
└── ...

officer-projection/src/test/java/com/knowit/policesystem/projection/
├── integration/         # Projection integration tests
│   ├── IntegrationTestBase.java
│   └── OfficerProjectionIntegrationTest.java
└── ...
```

## Edge Layer Testing

### What Edge Layer Tests Verify

1. **HTTP Response**: Status code, response body structure, error messages
2. **Kafka Event Production**: Event appears in correct topic with correct key
3. **Event Data**: All event fields match request data
4. **Event Metadata**: Event ID, timestamp, aggregate ID, version, event type
5. **NATS Event Production**: Critical events also appear in NATS (for dual-publish)
6. **Validation**: Invalid requests don't produce events
7. **Error Handling**: Appropriate HTTP status codes for different error conditions

### Edge Layer Test Example

```java
@Test
void testRegisterOfficer_WithValidData_ProducesEvent() throws Exception {
    // Given
    String badgeNumber = "12345";
    RegisterOfficerRequestDto request = new RegisterOfficerRequestDto(
        badgeNumber, "John", "Doe", "Officer",
        "john.doe@police.gov", "555-0100",
        LocalDate.of(2020, 1, 15), OfficerStatus.Active
    );
    
    // Prepare NATS subscription before API call
    JetStreamSubscription natsSubscription = natsHelper.prepareSubscription("commands.officer.register");
    natsSubscription.pull(1);
    
    // When
    String requestJson = objectMapper.writeValueAsString(request);
    mockMvc.perform(post("/api/v1/officers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.officerId").value(badgeNumber))
        .andExpect(jsonPath("$.message").value("Officer registration request created"));
    
    // Then - verify Kafka event
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
    assertThat(records).isNotEmpty();
    assertThat(records.count()).isEqualTo(1);
    
    ConsumerRecord<String, String> record = records.iterator().next();
    assertThat(record.key()).isEqualTo(badgeNumber);
    assertThat(record.topic()).isEqualTo(topicConfiguration.OFFICER_EVENTS);
    
    RegisterOfficerRequested event = eventObjectMapper.readValue(record.value(), RegisterOfficerRequested.class);
    assertThat(event.getEventId()).isNotNull();
    assertThat(event.getTimestamp()).isNotNull();
    assertThat(event.getAggregateId()).isEqualTo(badgeNumber);
    assertThat(event.getBadgeNumber()).isEqualTo(badgeNumber);
    assertThat(event.getFirstName()).isEqualTo("John");
    // ... verify all fields
    assertThat(event.getEventType()).isEqualTo("RegisterOfficerRequested");
    assertThat(event.getVersion()).isEqualTo(1);
    
    // Then - verify NATS event
    Thread.sleep(1000); // Wait for async publish
    natsSubscription.pull(1);
    Message natsMsg = natsSubscription.nextMessage(Duration.ofSeconds(5));
    assertThat(natsMsg).isNotNull();
    RegisterOfficerRequested natsEvent = eventObjectMapper.readValue(natsMsg.getData(), RegisterOfficerRequested.class);
    assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
}
```

### Validation Error Testing

Edge layer tests verify that validation errors prevent event production:

```java
@Test
void testRegisterOfficer_WithMissingBadgeNumber_Returns400() throws Exception {
    // Given
    RegisterOfficerRequestDto request = new RegisterOfficerRequestDto(
        null,  // Missing badgeNumber
        "John", "Doe", "Officer", "john.doe@police.gov", "555-0100",
        LocalDate.of(2020, 1, 15), OfficerStatus.Active
    );
    
    // When
    mockMvc.perform(post("/api/v1/officers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isBadRequest());
    
    // Then - verify no event in Kafka
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
    assertThat(records).isEmpty();
}
```

## Projection Layer Testing

### What Projection Layer Tests Verify

1. **Event Processing**: Events are consumed and processed correctly
2. **Projection State**: Database state matches expected projection
3. **Query Endpoints**: REST API returns correct data from projections
4. **Partial Updates**: Update events only modify specified fields
5. **History Tracking**: Status changes are recorded in history tables
6. **Idempotency**: Duplicate events don't corrupt state
7. **Dual-Publish Handling**: Events from both Kafka and NATS don't cause duplicates

### Projection Layer Test Example

```java
@Test
void registerEvent_shouldPersistProjectionAndExposeQuery() throws Exception {
    // Given
    String badge = "B-" + UUID.randomUUID();
    RegisterOfficerRequested event = new RegisterOfficerRequested(
        badge, "Jane", "Doe", "Sergeant",
        "jane.doe@example.com", "555-1234", "2023-01-01", "Active"
    );
    
    // When
    publishToKafka(event.getBadgeNumber(), event);
    
    // Then - verify via query endpoint
    ResponseEntity<Map> response = awaitOfficer(badge);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    
    Map<String, Object> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("badgeNumber")).isEqualTo(badge);
    assertThat(body.get("firstName")).isEqualTo("Jane");
    assertThat(body.get("lastName")).isEqualTo("Doe");
    assertThat(body.get("rank")).isEqualTo("Sergeant");
    // ... verify all fields
}
```

### Partial Update Testing

Projection tests verify that partial updates work correctly:

```java
@Test
void updateEvent_shouldApplyPartialUpdatesAndRetainExistingFields() throws Exception {
    // Given - register first
    String badge = "B-" + UUID.randomUUID();
    publishToKafka(badge, new RegisterOfficerRequested(
        badge, "John", "Smith", "Officer",
        "john.smith@example.com", "555-1111", "2023-02-02", "Active"
    ));
    
    // When - update only some fields
    UpdateOfficerRequested update = new UpdateOfficerRequested(
        badge, null, "Doe", "Lieutenant", "john.doe@example.com", null, null
    );
    publishToKafka(badge, update);
    
    // Then - verify partial update
    ResponseEntity<Map> response = awaitOfficer(badge);
    Map<String, Object> body = response.getBody();
    assertThat(body.get("firstName")).isEqualTo("John"); // unchanged
    assertThat(body.get("lastName")).isEqualTo("Doe");   // updated
    assertThat(body.get("rank")).isEqualTo("Lieutenant");  // updated
    assertThat(body.get("phoneNumber")).isEqualTo("555-1111"); // unchanged
}
```

### Idempotency Testing

Projection tests verify that duplicate events don't cause corruption:

```java
@Test
void changeStatus_shouldUpdateStatusAndAppendHistoryWithoutDuplicates() throws Exception {
    // Given
    String badge = "B-" + UUID.randomUUID();
    publishToKafka(badge, new RegisterOfficerRequested(...));
    
    // When - publish duplicate status change
    publishToKafka(badge, new ChangeOfficerStatusRequested(badge, "Inactive"));
    publishToKafka(badge, new ChangeOfficerStatusRequested(badge, "Inactive")); // duplicate
    publishToKafka(badge, new ChangeOfficerStatusRequested(badge, "OnLeave"));
    
    // Then - verify only unique status changes in history
    ResponseEntity<List> historyResponse = awaitHistory(badge);
    List<Map<String, Object>> history = historyResponse.getBody();
    assertThat(history).hasSize(2); // Not 3, duplicate filtered
    assertThat(history.get(0).get("status")).isEqualTo("Inactive");
    assertThat(history.get(1).get("status")).isEqualTo("OnLeave");
}
```

### Dual-Publish Testing

Projection tests verify that events from both Kafka and NATS don't cause duplicates:

```java
@Test
void natsEvent_shouldNotDoubleProcessWhenKafkaAlsoDelivers() throws Exception {
    // Given
    RegisterOfficerRequested event = new RegisterOfficerRequested(...);
    
    // When - publish to both Kafka and NATS
    publishToKafka(badge, event);
    
    String subject = EventClassification.generateNatsSubject(event);
    try (Connection connection = Nats.connect(nats.getNatsUrl())) {
        connection.publish(subject, objectMapper.writeValueAsBytes(event));
    }
    
    // Then - verify only one projection row exists
    ResponseEntity<Map> response = awaitOfficer(badge);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM officer_projection WHERE badge_number = ?", 
        Integer.class, badge);
    assertThat(count).isEqualTo(1);
}
```

## Unit Testing

### Infrastructure Component Tests

Unit tests verify infrastructure components in isolation:

**Command/Query Base Classes**:
```java
@Test
void testCommand_WithDefaultConstructor_GeneratesIdAndTimestamp() {
    TestCommand command = new TestCommand();
    assertThat(command.getCommandId()).isNotNull();
    assertThat(command.getTimestamp()).isNotNull();
    assertThat(command.getCommandType()).isEqualTo("TestCommand");
}
```

**Validation Framework**:
```java
@Test
void testValidationResult_WithErrors_IsNotValid() {
    ValidationError error1 = new ValidationError("field1", "Field is required", null);
    ValidationError error2 = new ValidationError("field2", "Invalid format", "invalid");
    ValidationResult result = ValidationResult.withErrors(List.of(error1, error2));
    
    assertThat(result.isValid()).isFalse();
    assertThat(result.hasErrors()).isTrue();
    assertThat(result.getErrors()).hasSize(2);
}
```

**Handler Infrastructure**:
```java
@Test
void testCommandHandler_CanProcessCommand() {
    TestCommand command = new TestCommand("aggregate-1", "test", 1);
    TestCommandHandler handler = new TestCommandHandler();
    
    handler.handle(command);
    
    assertThat(handler.getProcessedCommands()).contains(command);
}
```

## Running Tests

### Run All Tests

```bash
# Run all tests across all modules
mvn test

# Run tests for a specific module
mvn -pl edge test
mvn -pl operational-projection test
mvn -pl resource-projection test
mvn -pl workforce-projection test
```

### Run Specific Test Classes

```bash
# Run a specific test class
mvn -pl edge -Dtest=OfficerControllerTest test

# Run multiple test classes
mvn -pl edge -Dtest=OfficerControllerTest,VehicleControllerTest test
```

### Run Specific Test Methods

```bash
# Run a specific test method
mvn -pl edge -Dtest=OfficerControllerTest#testRegisterOfficer_WithValidData_ProducesEvent test
```

### Test Execution Notes

- **Container Startup**: Testcontainers will download Docker images on first run (may take time)
- **Shared Containers**: Containers are shared across test classes for performance
- **Parallel Execution**: Tests can run in parallel, but shared containers ensure no conflicts
- **Cleanup**: Containers are automatically cleaned up after all tests complete

## Best Practices

### 1. Test Independence

- Each test should be independent and not rely on other tests
- Use unique identifiers (UUIDs, timestamps) to avoid conflicts
- Clean up test data between tests when necessary

### 2. Descriptive Test Names

- Use clear, descriptive test names that explain what is being tested
- Follow the pattern: `test{Method}_{Condition}_{ExpectedResult}`
- Test names should serve as documentation

### 3. Comprehensive Assertions

- Verify all relevant aspects: HTTP response, event data, event metadata
- Use AssertJ for fluent, readable assertions
- Verify both positive and negative cases

### 4. Async Handling

- Use appropriate timeouts for async operations
- Use Awaitility for polling-based assertions
- Don't use fixed sleeps; use polling with timeouts

### 5. Test Data Management

- Use realistic test data that matches production scenarios
- Avoid hardcoded values that might conflict
- Use builders or factories for complex test objects

### 6. Error Testing

- Test all error conditions: validation errors, missing data, invalid formats
- Verify that errors don't produce events
- Verify appropriate HTTP status codes

### 7. Performance Considerations

- Use shared containers to reduce startup time
- Use "latest" offset for Kafka consumers to avoid processing old messages
- Clean up resources in `@AfterEach` methods

## Summary

The testing strategy for the Police Incident Management System emphasizes:

1. **Test-Driven Development**: Tests written before implementation
2. **Event-Driven Testing**: Tests verify event production and processing
3. **Real Infrastructure**: Tests use Testcontainers for realistic testing
4. **Comprehensive Coverage**: Happy paths, edge cases, and error conditions
5. **Clear Structure**: Given-When-Then pattern with descriptive names
6. **Layer-Specific Patterns**: Different patterns for edge vs projection layers

This approach ensures high-quality, maintainable code with comprehensive test coverage that serves as executable documentation.
