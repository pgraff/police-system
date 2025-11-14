# Increment 1.3: Command and Query Base Infrastructure - Detailed Plan

## Overview

This increment establishes the foundational infrastructure for handling commands and queries in the edge layer. It provides the base classes, handlers, validation framework, and error handling mechanisms that will be used by all subsequent domain-specific endpoints.

## Goals

1. Create base abstractions for Commands and Queries
2. Implement handler infrastructure for processing commands and queries
3. Establish validation framework for command/query validation
4. Create consistent error handling and response DTOs
5. Ensure all components are testable and follow Spring Boot best practices

## Architecture Context

Based on the system architecture:
- **Commands** represent write operations that produce events to Kafka
- **Queries** represent read operations (not implemented in this increment, but infrastructure prepared)
- Commands are validated before processing
- Validation errors return appropriate HTTP status codes
- Error responses follow a consistent structure
- The system is event-driven: commands produce events, not state changes

## Package Structure

```
com.knowit.policesystem.edge/
├── commands/
│   ├── Command.java                    # Base interface/abstract class
│   ├── CommandHandler.java              # Handler interface
│   ├── CommandHandlerRegistry.java     # Registry for command handlers
│   └── CommandResult.java              # Result wrapper for command execution
├── queries/
│   ├── Query.java                      # Base interface/abstract class
│   ├── QueryHandler.java               # Handler interface
│   └── QueryResult.java                # Result wrapper for query execution
├── validation/
│   ├── Validator.java                  # Validation interface
│   ├── ValidationResult.java           # Validation result container
│   ├── ValidationError.java            # Individual validation error
│   └── CommandValidator.java           # Base validator for commands
├── dto/
│   ├── ErrorResponse.java              # Standard error response DTO
│   ├── SuccessResponse.java            # Standard success response DTO
│   └── ValidationErrorResponse.java    # Validation error response DTO
└── exceptions/
    ├── ValidationException.java         # Exception for validation failures
    ├── CommandHandlerNotFoundException.java  # Exception when handler not found
    └── QueryHandlerNotFoundException.java    # Exception when handler not found
```

## Component Specifications

### 1. Command Base Class

**Location**: `com.knowit.policesystem.edge.commands.Command`

**Purpose**: Base abstraction for all commands in the system.

**Design Decision**: Use an abstract class (similar to Event) to provide common functionality.

**Fields**:
- `commandId` (UUID) - Unique identifier for the command
- `timestamp` (Instant) - When the command was created
- `aggregateId` (String) - The aggregate this command targets (optional)

**Methods**:
- `getCommandId()` - Returns the command ID
- `getTimestamp()` - Returns the timestamp
- `getAggregateId()` - Returns the aggregate ID
- `getCommandType()` - Abstract method returning command type name

**Constructor**:
- Default constructor that generates commandId and sets timestamp
- Constructor with aggregateId parameter

### 2. Query Base Class

**Location**: `com.knowit.policesystem.edge.queries.Query`

**Purpose**: Base abstraction for all queries in the system.

**Design Decision**: Use an abstract class (similar to Command and Event).

**Fields**:
- `queryId` (UUID) - Unique identifier for the query
- `timestamp` (Instant) - When the query was created

**Methods**:
- `getQueryId()` - Returns the query ID
- `getTimestamp()` - Returns the timestamp
- `getQueryType()` - Abstract method returning query type name

**Constructor**:
- Default constructor that generates queryId and sets timestamp

### 3. Command Handler Infrastructure

**Location**: `com.knowit.policesystem.edge.commands.CommandHandler`

**Purpose**: Interface for handling commands.

**Design Decision**: Generic interface with type parameter for command type.

**Interface**:
```java
public interface CommandHandler<C extends Command, R> {
    R handle(C command);
    Class<C> getCommandType();
}
```

**CommandHandlerRegistry**:
- Spring-managed component that maintains a map of command handlers
- Provides method to register handlers
- Provides method to find handler for a command type
- Throws `CommandHandlerNotFoundException` if handler not found

### 4. Query Handler Infrastructure

**Location**: `com.knowit.policesystem.edge.queries.QueryHandler`

**Purpose**: Interface for handling queries.

**Design Decision**: Generic interface with type parameter for query type.

**Interface**:
```java
public interface QueryHandler<Q extends Query, R> {
    R handle(Q query);
    Class<Q> getQueryType();
}
```

**QueryHandlerRegistry** (optional for this increment):
- Similar to CommandHandlerRegistry but for queries
- Can be implemented in future if needed

### 5. Validation Framework

**Location**: `com.knowit.policesystem.edge.validation`

**Purpose**: Provide validation infrastructure for commands and queries.

**Components**:

#### Validator Interface
```java
public interface Validator<T> {
    ValidationResult validate(T object);
}
```

#### ValidationResult
- `isValid()` - Returns true if validation passed
- `getErrors()` - Returns list of ValidationError objects
- `hasErrors()` - Returns true if there are errors

#### ValidationError
- `field` (String) - Field name that failed validation
- `message` (String) - Error message
- `rejectedValue` (Object) - The value that was rejected

#### CommandValidator
- Base abstract class for command validators
- Implements Validator<Command>
- Provides common validation utilities
- Subclasses implement domain-specific validation

**Integration with Spring Validation**:
- Use `@Valid` annotation on command fields
- Use Bean Validation annotations (`@NotNull`, `@NotBlank`, etc.)
- Custom validators can extend CommandValidator

### 6. Error Handling and DTOs

**Location**: `com.knowit.policesystem.edge.dto` and `com.knowit.policesystem.edge.exceptions`

**Purpose**: Provide consistent error responses and exception handling.

#### ErrorResponse DTO
```json
{
  "error": "Bad Request",
  "message": "Validation failed",
  "details": ["field1 is required", "field2 must be valid email"]
}
```

**Fields**:
- `error` (String) - Error type/category
- `message` (String) - Human-readable error message
- `details` (List<String>) - List of detailed error messages

#### SuccessResponse DTO
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... }
}
```

**Fields**:
- `success` (boolean) - Always true
- `message` (String) - Success message
- `data` (Object) - Response data (optional)

#### ValidationErrorResponse DTO
- Extends or uses ErrorResponse
- Specifically for validation errors
- Includes field-level error details

#### Exceptions

**ValidationException**:
- Runtime exception for validation failures
- Contains ValidationResult
- Can be caught and converted to ErrorResponse

**CommandHandlerNotFoundException**:
- Runtime exception when no handler found for command
- Returns 500 Internal Server Error

**QueryHandlerNotFoundException**:
- Runtime exception when no handler found for query
- Returns 500 Internal Server Error

### 7. Global Exception Handler

**Location**: `com.knowit.policesystem.edge.exceptions.GlobalExceptionHandler`

**Purpose**: Spring `@ControllerAdvice` to handle exceptions globally.

**Handles**:
- `ValidationException` → 400 Bad Request with ValidationErrorResponse
- `MethodArgumentNotValidException` (Spring) → 400 Bad Request with ValidationErrorResponse
- `CommandHandlerNotFoundException` → 500 Internal Server Error
- `QueryHandlerNotFoundException` → 500 Internal Server Error
- `IllegalArgumentException` → 400 Bad Request
- Generic exceptions → 500 Internal Server Error

## Test Strategy

Following the 8-step process, tests will be written first (Step 1) before implementation.

### Test Classes

1. **CommandTest** - Test Command base class
   - Test command ID generation
   - Test timestamp setting
   - Test aggregateId handling
   - Test command type method

2. **QueryTest** - Test Query base class
   - Test query ID generation
   - Test timestamp setting
   - Test query type method

3. **CommandHandlerTest** - Test command handler infrastructure
   - Test handler registration
   - Test handler lookup
   - Test handler execution
   - Test handler not found exception

4. **QueryHandlerTest** - Test query handler infrastructure
   - Test handler registration (if implemented)
   - Test handler lookup
   - Test handler execution

5. **ValidationFrameworkTest** - Test validation infrastructure
   - Test ValidationResult creation
   - Test ValidationError creation
   - Test validator interface
   - Test command validator base class

6. **ErrorHandlingTest** - Test error handling
   - Test ErrorResponse DTO
   - Test SuccessResponse DTO
   - Test ValidationErrorResponse DTO
   - Test exception to response conversion

7. **GlobalExceptionHandlerTest** - Test global exception handler
   - Test ValidationException handling
   - Test MethodArgumentNotValidException handling
   - Test CommandHandlerNotFoundException handling
   - Test generic exception handling

### Test Criteria (from Development Plan)

- ✅ Command can be validated
- ✅ Command handler can process commands
- ✅ Query handler can process queries
- ✅ Validation errors return appropriate HTTP status codes
- ✅ Error responses follow consistent structure

## Implementation Steps (Following 8-Step Process)

### Step 0: Write Requirements ✅
- Already documented in DEVELOPMENT_PLAN.md
- This detailed plan expands on those requirements

### Step 1: Write Tests
- Create all test classes listed above
- Write test methods for each component
- Tests should initially fail (red phase)
- Use JUnit 5, AssertJ, and Spring Boot Test

### Step 2: Write Implementation Code
- Implement Command base class
- Implement Query base class
- Implement CommandHandler interface and registry
- Implement QueryHandler interface (and registry if needed)
- Implement validation framework
- Implement DTOs
- Implement exceptions
- Implement GlobalExceptionHandler

### Step 3: Run Tests for the Feature
- Execute all tests for increment 1.3
- Verify all tests pass
- Fix any issues

### Step 4: Run All Tests (Regression Check)
- Execute full test suite
- Ensure no regressions
- Fix any broken tests

### Step 5: Update Development Plan
- Mark increment 1.3 as completed
- Document any deviations or decisions

### Step 6: Commit and Create Pull Request
- Commit with message: "feat: add command and query base infrastructure"
- Create PR with description

### Step 7: Write Technical Demo Suggestion
- Demo outline for showcasing the infrastructure

## Design Decisions and Rationale

1. **Abstract Classes vs Interfaces**: Using abstract classes for Command and Query (similar to Event) to provide common functionality and reduce boilerplate.

2. **Generic Handlers**: Using generics for type safety in handlers while maintaining flexibility.

3. **Spring Integration**: Leveraging Spring's dependency injection and validation framework rather than building custom solutions.

4. **Validation Framework**: Creating a simple validation framework that integrates with Spring Validation but provides additional structure for command validation.

5. **Error Response Structure**: Following the API documentation format for consistency.

6. **Exception Handling**: Using @ControllerAdvice for centralized exception handling, following Spring Boot best practices.

## Dependencies

- Spring Boot Web (already in pom.xml)
- Spring Boot Validation (already in pom.xml)
- Common module (Event classes)
- Jackson (for JSON serialization)
- JUnit 5, AssertJ, Spring Boot Test (for testing)

## Future Considerations

- Query handlers may not be fully utilized until read models are implemented
- Validation framework can be extended with custom validators
- Command/Query handlers can be enhanced with middleware/interceptors
- Error responses can be enhanced with error codes and internationalization

## Notes

- This increment focuses on infrastructure, not domain-specific implementations
- Commands will be used immediately in next increments (e.g., RegisterOfficerRequested)
- Queries infrastructure is prepared but may not be used until read models exist
- All components follow Spring Boot conventions and best practices
- Code should be well-documented with JavaDoc

