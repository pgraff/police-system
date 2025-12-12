# NATS Request-Response Implementation Assessment

## Overview

This document assesses the completeness of the NATS request-response implementation for projection queries, including test coverage, OpenAPI specification, and documentation.

## Test Coverage Assessment

### ❌ Missing Tests

#### 1. Unit Tests for NATS Query Infrastructure
**Location**: `common/src/test/java/com/knowit/policesystem/common/nats/`

**Missing**:
- `NatsQueryClientTest.java` - Test query client functionality
  - Test successful query requests
  - Test timeout handling
  - Test connection failures
  - Test disabled state
  - Test async query methods

- `QueryRequestTest.java` / `QueryResponseTest.java` - Test DTOs
  - Test serialization/deserialization
  - Test query ID generation

#### 2. Unit Tests for Projection Query Handlers
**Location**: `{projection}/src/test/java/com/knowit/policesystem/projection/nats/`

**Missing** (for all 6 projections):
- `OfficerNatsQueryHandlerTest.java`
- `CallNatsQueryHandlerTest.java`
- `IncidentNatsQueryHandlerTest.java`
- `DispatchNatsQueryHandlerTest.java`
- `ActivityNatsQueryHandlerTest.java`
- `AssignmentNatsQueryHandlerTest.java`

**Test Cases Needed**:
- Test exists query handling
- Test error handling
- Test disabled state
- Test subject subscription

#### 3. Unit Tests for Edge Services
**Location**: `edge/src/test/java/com/knowit/policesystem/edge/services/`

**Missing**:
- `ProjectionQueryServiceTest.java` - Test projection query service
  - Test successful existence checks
  - Test timeout handling
  - Test error handling

- `OfficerExistenceServiceTest.java` - Test officer existence service
- `OfficerConflictServiceTest.java` - Test conflict detection
- Similar tests for other domain services

#### 4. Integration Tests for 404/409 Responses
**Location**: `edge/src/test/java/com/knowit/policesystem/edge/controllers/`

**Missing Test Cases**:
- `OfficerControllerTest.java`:
  - `testUpdateOfficer_WithNonExistentBadgeNumber_Returns404()`
  - `testChangeOfficerStatus_WithNonExistentBadgeNumber_Returns404()`
  - `testRegisterOfficer_WithDuplicateBadgeNumber_Returns409()`

- `IncidentControllerTest.java`:
  - `testUpdateIncident_WithNonExistentIncidentId_Returns404()`
  - `testDispatchIncident_WithNonExistentIncidentId_Returns404()`

- `ActivityControllerTest.java`:
  - `testUpdateActivity_WithNonExistentActivityId_Returns404()`

- `AssignmentControllerTest.java`:
  - `testChangeAssignmentStatus_WithNonExistentAssignmentId_Returns404()`

- `CallControllerTest.java`:
  - Already has some 404 tests, but should verify they work with new NATS queries

#### 5. Integration Tests for NATS Request-Response
**Location**: `edge/src/test/java/com/knowit/policesystem/edge/integration/`

**Missing**:
- `NatsQueryIntegrationTest.java` - End-to-end test
  - Test edge → NATS → projection → NATS → edge flow
  - Test with real projection service running
  - Test timeout scenarios
  - Test projection unavailable scenarios

### ✅ Existing Test Infrastructure

- `BaseIntegrationTest.java` - Provides Kafka and NATS test containers
- `NatsTestHelper.java` - Helper for NATS testing
- Controller tests follow consistent pattern
- Test infrastructure supports adding new tests

## OpenAPI Specification Assessment

### ✅ Current Status

**Good Coverage**:
- `POST /officers` - Has 409 Conflict response documented
- `PUT /officers/{badgeNumber}` - Has 404 Not Found response documented
- `PATCH /officers/{badgeNumber}/status` - Has 404 Not Found response documented
- Most update endpoints have 404 responses documented

**Components**:
- `components/responses/NotFound` - Defined and referenced
- `components/schemas/ErrorResponse` - Defined for error responses

### ⚠️ Potential Gaps

**Need to Verify**:
1. All endpoints that now return 404 have it documented:
   - `PUT /incidents/{incidentId}` - Should have 404
   - `POST /incidents/{incidentId}/dispatch` - Should have 404
   - `PUT /activities/{activityId}` - Should have 404
   - `PATCH /assignments/{assignmentId}/status` - Should have 404
   - Other update/status change endpoints

2. Conflict responses (409) - Only documented for `POST /officers`
   - Should document if other create endpoints can return 409

3. Response descriptions could be more specific:
   - Current: "Resource not found"
   - Better: "Resource not found. The specified {resource} does not exist in the projection."

### Recommendation

Run a systematic check to ensure all endpoints that use existence services have 404 documented, and all create endpoints that check for conflicts have 409 documented.

## Documentation Assessment

### ❌ Missing Documentation

#### 1. Architecture Documentation
**Files to Update**:

- `doc/architecture/data-flow.md`:
  - **Missing**: Description of synchronous query flow via NATS
  - **Should Add**: Section explaining how edge queries projections synchronously for existence checks
  - **Should Add**: Diagram showing query flow (edge → NATS → projection → NATS → edge)

- `doc/architecture/cqrs-design.md`:
  - **Missing**: Mention of synchronous projection queries
  - **Should Add**: Note about query capabilities alongside command flow

- `doc/architecture/components.md`:
  - **Missing**: Description of NATS query handlers in projections
  - **Missing**: Description of ProjectionQueryService in edge
  - **Should Add**: Components for query infrastructure

#### 2. API Documentation
**File**: `doc/api/README.md`

**Current Status**: ✅ Mentions 404 and 409 status codes

**Missing**:
- Explanation of when 404 is returned (resource doesn't exist in projection)
- Explanation of when 409 is returned (duplicate resource)
- Note about eventual consistency implications
- Example of 404 error response
- Example of 409 error response

**Should Add Section**:
```markdown
## Resource Existence and Conflict Detection

The API now supports synchronous resource existence checks via NATS request-response queries to projections. This enables:

- **404 Not Found**: Returned when attempting to update or modify a resource that doesn't exist in the projection
- **409 Conflict**: Returned when attempting to create a resource that already exists (e.g., duplicate badge number)

### Eventual Consistency Note

Projections are eventually consistent. A resource may not appear in the projection immediately after creation. The edge queries projections synchronously, so:
- Recently created resources may return 404 if queried too quickly
- This is expected behavior in an eventually consistent system
- Clients should handle 404 responses appropriately
```

#### 3. Projection Documentation
**Files**: `{projection}/README.md` (if they exist)

**Missing**:
- Documentation of NATS query handler capabilities
- Query subject patterns
- Configuration for query handlers

### ✅ Existing Documentation

- API README mentions status codes
- Architecture docs describe event-driven flow
- OpenAPI spec has response definitions

## Recommendations

### Priority 1: Critical (Do Before Production)

1. **Add Integration Tests for 404/409 Responses**
   - Test that validators properly throw exceptions
   - Test that exceptions are converted to correct HTTP status codes
   - Test with real projection services

2. **Verify OpenAPI Spec Completeness**
   - Audit all endpoints for 404/409 documentation
   - Add missing response definitions
   - Improve response descriptions

3. **Update Architecture Documentation**
   - Document synchronous query flow in `data-flow.md`
   - Add query components to `components.md`

### Priority 2: Important (Should Do Soon)

4. **Add Unit Tests for Core Components**
   - Test `NatsQueryClient` thoroughly
   - Test `ProjectionQueryService`
   - Test existence services

5. **Add Integration Test for NATS Request-Response**
   - End-to-end test of query flow
   - Test error scenarios

6. **Update API Documentation**
   - Add section on resource existence checks
   - Add examples of 404/409 responses
   - Document eventual consistency implications

### Priority 3: Nice to Have

7. **Add Unit Tests for Query Handlers**
   - Test each projection's query handler
   - Test error handling

8. **Add Projection-Specific Documentation**
   - Document query capabilities in projection READMEs

## Summary

### Test Coverage: ⚠️ **Insufficient**
- **Missing**: Unit tests for query infrastructure
- **Missing**: Integration tests for 404/409 responses
- **Missing**: End-to-end NATS query tests
- **Has**: Good test infrastructure to build upon

### OpenAPI Spec: ✅ **Mostly Complete**
- **Has**: 404 and 409 responses documented for key endpoints
- **Needs**: Verification that all updated endpoints have proper documentation
- **Needs**: More descriptive response messages

### Documentation: ⚠️ **Needs Updates**
- **Missing**: Architecture documentation of query flow
- **Missing**: API documentation explaining 404/409 behavior
- **Has**: Basic status code documentation

## Next Steps

1. Create test plan for missing tests
2. Audit OpenAPI spec for completeness
3. Update architecture documentation
4. Add API documentation section on resource existence

