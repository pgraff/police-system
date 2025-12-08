# Development Process Rules

This document defines the incremental development process that must be followed for all feature development.

## System Architecture Approach

### Event-Driven Edge Layer
The system follows an event-driven architecture where:
- **Edge servers** receive HTTP requests (commands) and produce events to Kafka
- **Events represent requests/commands** from the edge, not state changes
- **No state reconstruction** in the edge layer - events are simply produced to Kafka
- **No CQRS projections** in initial development - focus is on event production
- **Tests verify Kafka message production** - call API endpoint and verify event appears in Kafka

### Event Naming Convention
Events represent **requests** or **commands** from the edge, not state changes:
- ✅ `RegisterOfficerRequested` - Request to register an officer
- ✅ `UpdateOfficerRequested` - Request to update officer information
- ✅ `ChangeOfficerStatusRequested` - Request to change officer status
- ✅ `ReportIncidentRequested` - Request to report an incident
- ✅ `DispatchIncidentRequested` - Request to dispatch units to incident
- ❌ `OfficerUpdated` - This implies state change, not a request
- ❌ `IncidentUpdated` - This implies state change, not a request

### Testing Strategy
All tests follow this pattern:
1. **Call the REST API endpoint** with appropriate request data
2. **Verify the event is produced to Kafka** with correct event data
3. **No state verification** - we only verify event production
4. Tests use Kafka test containers or embedded Kafka for isolation

## Process Overview

All development work must be organized into clear increments. Each increment follows a strict 8-step process that ensures quality, testability, and maintainability.

## Increment Development Steps

### Step 0: Write Requirements
- Document the requirements for the increment clearly and concisely
- Specify what the feature should do, not how it should be implemented
- Include acceptance criteria that can be verified
- Define the REST API endpoint (method, path, request/response structure)
- Define the Kafka event that should be produced
- Specify test criteria (what Kafka message to verify)
- Update any relevant planning documents or issue trackers

### Step 1: Write Tests
- Write test cases **before** implementing the feature (Test-Driven Development)
- Tests must follow the event-driven testing pattern:
  - Call REST API endpoint
  - Verify event is produced to Kafka
  - Verify event contains correct data
- Tests should cover:
  - Happy path scenarios
  - Edge cases (invalid input, missing fields)
  - Error conditions (validation failures)
- Tests must be executable and initially fail (red phase)
- Use descriptive test names that explain what is being tested
- Use Kafka test containers or embedded Kafka for testing

### Step 2: Write Implementation Code
- Implement the REST API endpoint (controller)
- Implement command handler that validates input
- Implement event producer that publishes to Kafka
- **Do NOT implement state reconstruction or projections**
- Follow existing code patterns and conventions in the codebase
- Keep the implementation focused on passing the tests (green phase)
- Refactor if needed, but ensure tests continue to pass

### Step 3: Run Tests for the Feature
- Execute the tests written in Step 1
- All tests for the new feature must pass
- Verify that events are correctly produced to Kafka
- Verify that event data matches expectations
- Document any test failures and fix them before proceeding

### Step 4: Run All Tests (Regression Check)
- Execute the complete test suite for the entire project
- Ensure no existing functionality has been broken
- Fix any regressions before proceeding
- All tests across the codebase must pass

### Step 5: Update Development Plan
- Mark the feature as completed in the development plan
- Update any relevant documentation
- Note any deviations from the original requirements
- Document any technical decisions or trade-offs made

### Step 6: Commit and Create Pull Request
- Commit the changes with a clear, descriptive commit message
- Follow the project's commit message conventions
- Push the changes and create a pull request
- Include in the PR description:
  - What was implemented
  - REST API endpoint details
  - Kafka event produced
  - How to test the feature (API call and expected Kafka message)
  - Any breaking changes (if applicable)
  - Reference to related issues or requirements

### Step 7: Write Technical Demo Suggestion
- Create a brief technical demo outline (maximum 5 minutes)
- The demo should showcase:
  - The REST API endpoint (show request/response)
  - The Kafka event produced (show event in Kafka topic)
  - How to verify the event was produced
  - Key technical aspects or patterns used
- Format the demo as a step-by-step guide suitable for pair programming sessions
- Include:
  - Exact API call to make (curl command or Postman example)
  - How to check Kafka topic for the event
  - What to highlight about the event structure
  - Any interesting technical details

## Rules and Guidelines

### Strict Adherence
- **Never skip steps** - Each step must be completed before moving to the next
- **Never commit code without tests** - All code must have corresponding tests
- **Never commit failing tests** - All tests must pass before committing
- **Never skip the regression check** - Step 4 is mandatory

### Quality Standards
- Code must be clean, readable, and maintainable
- Tests must be meaningful and provide real value
- Documentation must be kept up to date
- Code reviews should focus on both code quality and test coverage

### Increment Boundaries
- Each increment should be small enough to complete within a reasonable timeframe
- Increments should be independent when possible, or clearly document dependencies
- If an increment is too large, break it down into smaller sub-increments

### Process Exceptions
- Only in exceptional circumstances (e.g., critical production bugs) may steps be reordered or skipped
- Any exceptions must be documented and justified
- Normal development must always follow the full process

## Example Workflow

```
Increment: Register Officer Endpoint

Step 0: Requirements
- REST API: POST /api/officers
- Request body: { badgeNumber, firstName, lastName, rank, email, phoneNumber, hireDate, status }
- Response: 201 Created with officer ID
- Produces event: RegisterOfficerRequested to Kafka topic "officer-events"
- Test criteria: Verify RegisterOfficerRequested event appears in Kafka with correct data

Step 1: Write Tests
- testRegisterOfficer_WithValidData_ProducesEvent()
  - Call POST /api/officers with valid data
  - Verify RegisterOfficerRequested event in Kafka
  - Verify event contains all officer data
- testRegisterOfficer_WithMissingBadgeNumber_Returns400()
  - Call POST /api/officers without badgeNumber
  - Verify 400 Bad Request
  - Verify no event produced
- testRegisterOfficer_WithDuplicateBadgeNumber_Returns409()
  - Call POST /api/officers with existing badgeNumber
  - Verify 409 Conflict
  - Verify no event produced

Step 2: Write Implementation
- Create OfficerController with POST endpoint
- Create RegisterOfficerCommand and handler
- Create RegisterOfficerRequested event class
- Implement Kafka event producer
- Add validation logic

Step 3: Run Feature Tests
- All officer registration tests pass ✓
- Events correctly produced to Kafka ✓

Step 4: Run All Tests
- Full test suite passes ✓

Step 5: Update Plan
- Mark "Register Officer Endpoint" as completed

Step 6: Commit & PR
- Commit: "feat: add register officer endpoint"
- PR description includes API endpoint, event details, and test instructions

Step 7: Demo Suggestion
1. Show POST /api/officers request with curl or Postman
2. Show 201 Created response
3. Show RegisterOfficerRequested event in Kafka topic using kafka-console-consumer
4. Highlight event structure and data
5. Show validation error example (missing badgeNumber)
6. Explain event-driven architecture approach

Step 8: Update the development plan (DEVELOPMENT_PLAN.md) to mark off what has been done
```

