# Test Failures Summary

## Overall Status
- **Total Tests:** 407
- **Failures:** 4
- **Errors:** 3
- **Skipped:** 0

## Failures (4)

### 1. NatsQueryIntegrationTest.lambda$testGetQuery_EdgeToProjection_ReturnsData$1:192
- **Issue:** Status expected:<200> but was:<404>
- **Type:** HTTP status mismatch
- **Likely Cause:** Projection not returning data or endpoint not found

### 2. ConcurrentQueryE2ETest.testConcurrentQueries_HandlesMultipleRequests:177
- **Issue:** Expected all elements to match predicate but got all 404s
- **Type:** All queries returning 404
- **Likely Cause:** Projection queries failing or projection not properly initialized

### 3. ConcurrentQueryE2ETest.testConcurrentQueries_WithMixedExistence_HandlesCorrectly:262
- **Issue:** expected: 5L but was: 0L
- **Type:** Count mismatch
- **Likely Cause:** Queries not finding expected entities

### 4. QueryPerformanceE2ETest.testQueryLatency_MeetsSLA:132
- **Issue:** Status expected:<200> but was:<404>
- **Type:** HTTP status mismatch
- **Likely Cause:** Same as #1 - projection query failing

## Errors (3)

### 1. OfficerExistenceQueryE2ETest.testExistsQuery_WhenOfficerExists_ReturnsTrue
- **Issue:** ConditionTimeout - Condition not fulfilled within 1 minute
- **Type:** Timeout waiting for officer to exist
- **Likely Cause:** Event not processed or projection not updating

### 2. OfficerGetQueryE2ETest.testGetQuery_WhenOfficerDoesNotExist_ReturnsNull
- **Issue:** SQL grammar error
- **Error:** `PreparedStatementCallback; bad SQL grammar [SELECT badge_number, first_name, last_name, rank, email, phone_number, hire_date, status, updated_at FROM officer_projection WHERE badge_number = ?]`
- **Likely Cause:** Table `officer_projection` doesn't exist in test database OR schema not initialized

### 3. OfficerGetQueryE2ETest.testGetQuery_WhenOfficerExists_ReturnsFullData
- **Issue:** Same SQL grammar error as #2
- **Error:** Same SQL query failing
- **Likely Cause:** Same as #2 - schema initialization issue

## Additional Issues Found

### Database Schema Issue
- **Error:** `ERROR: relation "resource_assignment_projection" does not exist`
- **Location:** Operational projection Kafka listener
- **Likely Cause:** Schema not fully initialized in test environment

## Root Cause Analysis

The main issues appear to be:
1. **Schema initialization problems** - Tables not being created in test databases
2. **Projection startup issues** - Projections not properly initialized in E2E tests
3. **Event processing delays** - Events not being processed in time for assertions

## Next Steps

1. Check schema initialization in ProjectionTestContext
2. Verify table creation in test databases
3. Check projection startup and readiness in E2E tests
4. Review event processing timing in tests
