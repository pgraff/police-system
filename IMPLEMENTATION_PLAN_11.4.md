# Implementation Plan: Increment 11.4 - Record Shift Change Endpoint

## Overview
This document outlines the detailed implementation plan for the Record Shift Change Endpoint feature.

## Requirements Summary
- **REST API**: `POST /api/v1/shifts/{shiftId}/shift-changes`
- **Request body**: `{ shiftChangeId, changeTime, changeType, notes }`
- **Response**: `201 Created` with `{ shiftChangeId }`
- **Event**: `RecordShiftChangeRequested` to Kafka topic `shift-events` (keyed by shiftId)
- **Validation**: shiftChangeId required, changeType enum (Briefing, Handoff, Status, Other)
- **Test criteria**: Verify `RecordShiftChangeRequested` event appears in Kafka with correct data

## Implementation Steps

### Step 1: Create ChangeType Enum
**File**: `edge/src/main/java/com/knowit/policesystem/edge/domain/ChangeType.java`
- Enum values: Briefing, Handoff, Status, Other
- Follow pattern from ShiftStatus enum

### Step 2: Create Request DTO
**File**: `edge/src/main/java/com/knowit/policesystem/edge/dto/RecordShiftChangeRequestDto.java`
- Fields: shiftChangeId (required), changeTime (Instant, optional), changeType (ChangeType enum, required), notes (String, optional)
- Use @NotNull validation annotations
- Use @JsonFormat for changeTime (ISO-8601)

### Step 3: Create Response DTO
**File**: `edge/src/main/java/com/knowit/policesystem/edge/dto/ShiftChangeResponseDto.java`
- Field: shiftChangeId (String)
- Follow pattern from ShiftResponseDto

### Step 4: Create Command
**File**: `edge/src/main/java/com/knowit/policesystem/edge/commands/shifts/RecordShiftChangeCommand.java`
- Extends Command
- Fields: shiftId, shiftChangeId, changeTime, changeType, notes
- Constructor takes aggregateId (shiftId) and DTO
- Follow pattern from ChangeShiftStatusCommand

### Step 5: Create Validator
**File**: `edge/src/main/java/com/knowit/policesystem/edge/validation/shifts/RecordShiftChangeCommandValidator.java`
- Validates shiftId is not null/empty
- Validates shiftChangeId is not null/empty
- Validates changeType is not null
- Follow pattern from ChangeShiftStatusCommandValidator

### Step 6: Create Event
**File**: `common/src/main/java/com/knowit/policesystem/common/events/shifts/RecordShiftChangeRequested.java`
- Extends Event
- Fields: shiftId, shiftChangeId, changeTime (Instant), changeType (String), notes (String)
- Constructor takes all fields, uses shiftId as aggregateId
- getEventType() returns "RecordShiftChangeRequested"
- Follow pattern from ChangeShiftStatusRequested

### Step 7: Create Command Handler
**File**: `edge/src/main/java/com/knowit/policesystem/edge/commands/shifts/RecordShiftChangeCommandHandler.java`
- Implements CommandHandler<RecordShiftChangeCommand, ShiftChangeResponseDto>
- Publishes RecordShiftChangeRequested to "shift-events" topic
- Uses shiftId as Kafka key
- Converts changeType enum to string
- Returns ShiftChangeResponseDto with shiftChangeId
- Register with CommandHandlerRegistry in @PostConstruct
- Follow pattern from ChangeShiftStatusCommandHandler

### Step 8: Add Controller Endpoint
**File**: `edge/src/main/java/com/knowit/policesystem/edge/controllers/ShiftController.java`
- Add RecordShiftChangeCommandValidator as dependency
- Add POST endpoint: `/api/v1/shifts/{shiftId}/shift-changes`
- Validate command, handle, return 201 Created with ShiftChangeResponseDto
- Follow pattern from changeShiftStatus method

### Step 9: Write Tests
**File**: `edge/src/test/java/com/knowit/policesystem/edge/controllers/ShiftControllerTest.java`

#### Test 1: testRecordShiftChange_WithValidData_ProducesEvent()
- Call POST /api/v1/shifts/{shiftId}/shift-changes with valid data
- Verify 201 Created response
- Verify response contains shiftChangeId
- Verify RecordShiftChangeRequested event in Kafka
- Verify event contains: shiftId, shiftChangeId, changeTime, changeType, notes
- Verify shiftId used as Kafka key
- Verify event type is "RecordShiftChangeRequested"

#### Test 2: testRecordShiftChange_WithMissingShiftChangeId_Returns400()
- Call POST /api/v1/shifts/{shiftId}/shift-changes without shiftChangeId
- Verify 400 Bad Request
- Verify no event produced

#### Test 3: testRecordShiftChange_WithInvalidChangeType_Returns400()
- Call POST /api/v1/shifts/{shiftId}/shift-changes with invalid changeType enum
- Verify 400 Bad Request
- Verify no event produced

## Testing Strategy
- All tests follow the event-driven testing pattern
- Call REST API endpoint
- Verify event is produced to Kafka
- Verify event contains correct data
- Use Kafka test containers for isolation

## Files to Create/Modify

### New Files:
1. `edge/src/main/java/com/knowit/policesystem/edge/domain/ChangeType.java`
2. `edge/src/main/java/com/knowit/policesystem/edge/dto/RecordShiftChangeRequestDto.java`
3. `edge/src/main/java/com/knowit/policesystem/edge/dto/ShiftChangeResponseDto.java`
4. `edge/src/main/java/com/knowit/policesystem/edge/commands/shifts/RecordShiftChangeCommand.java`
5. `edge/src/main/java/com/knowit/policesystem/edge/validation/shifts/RecordShiftChangeCommandValidator.java`
6. `edge/src/main/java/com/knowit/policesystem/edge/commands/shifts/RecordShiftChangeCommandHandler.java`
7. `common/src/main/java/com/knowit/policesystem/common/events/shifts/RecordShiftChangeRequested.java`

### Modified Files:
1. `edge/src/main/java/com/knowit/policesystem/edge/controllers/ShiftController.java`
2. `edge/src/test/java/com/knowit/policesystem/edge/controllers/ShiftControllerTest.java`
3. `DEVELOPMENT_PLAN.md`

## Success Criteria
- All tests pass
- Event is correctly produced to Kafka with shiftId as key
- Event contains all required fields
- Validation works correctly
- Code follows existing patterns and conventions
- No regressions in existing functionality
