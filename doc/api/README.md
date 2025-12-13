# Police System API Documentation

This directory contains the OpenAPI specification for the Police Incident Management System REST API.

## Files

- `openapi.yaml` - Complete OpenAPI 3.0.3 specification for all API endpoints

## Using the API Specification

### Viewing the API Documentation

You can view and interact with the API specification using several tools:

#### 1. Swagger UI (Recommended)

If you have Swagger UI installed or running:

```bash
# Using Docker
docker run -p 8080:8080 -e SWAGGER_JSON=/api/openapi.yaml -v $(pwd)/doc/api:/api swaggerapi/swagger-ui

# Then open http://localhost:8080 in your browser
```

#### 2. Swagger Editor

Use the online Swagger Editor:
1. Go to https://editor.swagger.io/
2. File → Import File → Select `openapi.yaml`
3. View and edit the specification

#### 3. Postman

Import the OpenAPI specification into Postman:
1. Open Postman
2. Import → File → Select `openapi.yaml`
3. All endpoints will be imported as a collection

#### 4. Insomnia

Import the OpenAPI specification into Insomnia:
1. Open Insomnia
2. Create → Import → From File → Select `openapi.yaml`
3. All endpoints will be imported

#### 5. Redoc

Generate beautiful documentation with Redoc:

```bash
# Using Docker
docker run -p 8080:80 -v $(pwd)/doc/api:/usr/share/nginx/html/api:ro redocly/redoc

# Then open http://localhost:8080/api/openapi.yaml
```

### Validating the Specification

Validate the OpenAPI specification:

```bash
# Using swagger-cli
npm install -g @apidevtools/swagger-cli
swagger-cli validate doc/api/openapi.yaml

# Using openapi-cli
npm install -g @redocly/cli
redocly lint doc/api/openapi.yaml
```

## API Overview

The Police System API is an event-driven REST API where:

- All operations produce events to Kafka
- Events represent requests/commands, not state changes
- No state reconstruction in the edge layer
- All operations are asynchronous via Kafka events

### Base URL

- Local: `http://localhost:8080/api`
- Production: `https://api.policesystem.example.com/api`

### Authentication

Currently, the API does not require authentication (to be implemented in future increments).

### Response Format

All successful responses return JSON with appropriate status codes:
- `201 Created` - Resource created
- `200 OK` - Request processed successfully
- `400 Bad Request` - Validation error
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource conflict (e.g., duplicate badge number)

### Error Format

Error responses follow this structure:

```json
{
  "error": "Bad Request",
  "message": "Validation failed",
  "details": [
    "badgeNumber is required",
    "email must be a valid email address"
  ]
}
```

### Resource Existence and Conflict Detection

The API supports synchronous resource existence checks via NATS request-response queries to projections. This enables immediate feedback on resource state:

#### 404 Not Found

Returned when attempting to update, modify, or perform operations on a resource that doesn't exist in the projection.

**Example 404 Response**:
```json
{
  "error": "Not Found",
  "message": "Officer not found: BADGE-999",
  "details": []
}
```

**Endpoints that return 404**:
- `PUT /officers/{badgeNumber}` - When officer doesn't exist
- `PATCH /officers/{badgeNumber}/status` - When officer doesn't exist
- `PUT /incidents/{incidentId}` - When incident doesn't exist
- `POST /incidents/{incidentId}/dispatch` - When incident doesn't exist
- `PUT /activities/{activityId}` - When activity doesn't exist
- `PATCH /assignments/{assignmentId}/status` - When assignment doesn't exist
- Similar update/status change endpoints for other domains

#### 409 Conflict

Returned when attempting to create a resource that already exists (e.g., duplicate badge number).

**Example 409 Response**:
```json
{
  "error": "Conflict",
  "message": "Officer with badge number already exists: 12345",
  "details": []
}
```

**Endpoints that return 409**:
- `POST /officers` - When badge number already exists

#### Eventual Consistency Note

Projections are eventually consistent. A resource may not appear in the projection immediately after creation. The edge queries projections synchronously via NATS, so:

- **Recently created resources** may return 404 if queried too quickly after creation
- This is expected behavior in an eventually consistent system
- Clients should handle 404 responses appropriately (e.g., retry after a short delay)
- The resource will be available once the projection has processed the creation event

**Best Practice**: When creating a resource and then immediately updating it, add a small delay or use the projection's REST API to verify the resource exists before updating.

## API Endpoints by Domain

### Officers
- `POST /officers` - Register a new officer
- `PUT /officers/{badgeNumber}` - Update officer information
- `PATCH /officers/{badgeNumber}/status` - Change officer status

### Vehicles
- `POST /vehicles` - Register a new vehicle
- `PUT /vehicles/{unitId}` - Update vehicle information
- `PATCH /vehicles/{unitId}/status` - Change vehicle status

### Units
- `POST /units` - Create a new unit
- `PUT /units/{unitId}` - Update unit information
- `PATCH /units/{unitId}/status` - Change unit status

### Persons
- `POST /persons` - Register a new person
- `PUT /persons/{personId}` - Update person information

### Locations
- `POST /locations` - Create a new location
- `PUT /locations/{locationId}` - Update location information
- `POST /incidents/{incidentId}/locations` - Link location to incident
- `DELETE /incidents/{incidentId}/locations/{locationId}` - Unlink location from incident
- `POST /calls/{callId}/locations` - Link location to call
- `DELETE /calls/{callId}/locations/{locationId}` - Unlink location from call

### Incidents
- `POST /incidents` - Report a new incident
- `PUT /incidents/{incidentId}` - Update incident information
- `PATCH /incidents/{incidentId}/status` - Change incident status
- `POST /incidents/{incidentId}/dispatch` - Dispatch incident
- `POST /incidents/{incidentId}/arrive` - Arrive at incident
- `POST /incidents/{incidentId}/clear` - Clear incident

### Calls
- `POST /calls` - Receive a new call
- `PUT /calls/{callId}` - Update call information
- `PATCH /calls/{callId}/status` - Change call status
- `POST /calls/{callId}/dispatch` - Dispatch call
- `POST /calls/{callId}/arrive` - Arrive at call
- `POST /calls/{callId}/clear` - Clear call
- `POST /calls/{callId}/incidents` - Link call to incident
- `POST /calls/{callId}/dispatches` - Link call to dispatch

### Activities
- `POST /activities` - Start a new activity
- `PUT /activities/{activityId}` - Update activity information
- `PATCH /activities/{activityId}/status` - Change activity status
- `POST /activities/{activityId}/complete` - Complete activity
- `POST /activities/{activityId}/incidents` - Link activity to incident

### Assignments
- `POST /assignments` - Create a new assignment
- `PATCH /assignments/{assignmentId}/status` - Change assignment status
- `POST /assignments/{assignmentId}/complete` - Complete assignment
- `POST /assignments/{assignmentId}/dispatches` - Link assignment to dispatch
- `POST /assignments/{assignmentId}/resources` - Assign resource to assignment
- `DELETE /assignments/{assignmentId}/resources/{resourceId}` - Unassign resource from assignment
- `PATCH /assignments/{assignmentId}/resources/{resourceId}/status` - Change resource assignment status

### Shifts
- `POST /shifts` - Start a new shift
- `PATCH /shifts/{shiftId}/status` - Change shift status
- `POST /shifts/{shiftId}/end` - End shift
- `POST /shifts/{shiftId}/shift-changes` - Record shift change
- `PUT /shifts/{shiftId}/officers/{badgeNumber}` - Update officer shift
- `POST /shifts/{shiftId}/officers/{badgeNumber}/check-in` - Check in officer to shift
- `POST /shifts/{shiftId}/officers/{badgeNumber}/check-out` - Check out officer from shift

### Dispatches
- `POST /dispatches` - Create a new dispatch
- `PATCH /dispatches/{dispatchId}/status` - Change dispatch status

### Involved Parties
- `POST /involved-parties` - Involve party in incident/call/activity
- `PUT /involved-parties/{involvementId}` - Update party involvement
- `POST /involved-parties/{involvementId}/end` - End party involvement

## Projection Query APIs

✅ **Implemented**: 3 consolidated projection services expose query APIs for read-only access to projected data. Each projection service runs as a separate deployable service (future K8s pod).

### Base URLs

**Consolidated Projections:**
- Operational Projection: `http://localhost:8081/api/projections/` (handles: incidents, calls, dispatches, activities, assignments, involved parties, resource assignments)
- Resource Projection: `http://localhost:8082/api/projections/` (handles: officers, vehicles, units, persons, locations)
- Workforce Projection: `http://localhost:8083/api/projections/` (handles: shifts, officer shifts, shift changes)

**Legacy Individual Projections (Deprecated):**
- Officer Projection: `http://localhost:8081/api/projections/officers` (deprecated, use resource-projection)
- Incident Projection: `http://localhost:8082/api/projections/incidents` (deprecated, use operational-projection)
- Call Projection: `http://localhost:8083/api/projections/calls` (deprecated, use operational-projection)
- Dispatch Projection: `http://localhost:8084/api/projections/dispatches` (deprecated, use operational-projection)
- Activity Projection: `http://localhost:8085/api/projections/activities` (deprecated, use operational-projection)
- Assignment Projection: `http://localhost:8086/api/projections/assignments` (deprecated, use operational-projection)

### Common Query Patterns

All projection services support:
- `GET /{id}` - Get single entity by ID
- `GET /{id}/history` - Get status change history for entity
- `GET /` - List entities with filtering and pagination

### Operational Projection (Port 8081)

**Incidents:**
- `GET /api/projections/incidents/{incidentId}` - Get incident by ID
- `GET /api/projections/incidents/{incidentId}/history` - Get incident status history
- `GET /api/projections/incidents?status={status}&priority={priority}&page={page}&size={size}` - List incidents with filters
- `GET /api/projections/incidents/{incidentId}/full` - Get incident with all related data (composite query)

**Calls:**
- `GET /api/projections/calls/{callId}` - Get call by ID
- `GET /api/projections/calls/{callId}/history` - Get call status history
- `GET /api/projections/calls?status={status}&priority={priority}&incidentId={id}&page={page}&size={size}` - List calls with filters

**Dispatches:**
- `GET /api/projections/dispatches/{dispatchId}` - Get dispatch by ID
- `GET /api/projections/dispatches/{dispatchId}/history` - Get dispatch status history
- `GET /api/projections/dispatches?status={status}&callId={id}&page={page}&size={size}` - List dispatches with filters

**Activities:**
- `GET /api/projections/activities/{activityId}` - Get activity by ID
- `GET /api/projections/activities/{activityId}/history` - Get activity status history
- `GET /api/projections/activities?status={status}&activityType={type}&incidentId={id}&callId={id}&page={page}&size={size}` - List activities with filters

**Assignments:**
- `GET /api/projections/assignments/{assignmentId}` - Get assignment by ID
- `GET /api/projections/assignments/{assignmentId}/history` - Get assignment status history
- `GET /api/projections/assignments?status={status}&dispatchId={id}&incidentId={id}&callId={id}&page={page}&size={size}` - List assignments with filters

**Involved Parties:**
- `GET /api/projections/involved-parties/{involvementId}` - Get involved party by ID
- `GET /api/projections/involved-parties?personId={id}&incidentId={id}&callId={id}&activityId={id}&page={page}&size={size}` - List involved parties with filters

**Resource Assignments:**
- `GET /api/projections/resource-assignments/{id}` - Get resource assignment by ID
- `GET /api/projections/resource-assignments?assignmentId={id}&resourceId={id}&resourceType={type}&page={page}&size={size}` - List resource assignments with filters

### Resource Projection (Port 8082)

**Officers:**
- `GET /api/projections/officers/{badgeNumber}` - Get officer by badge number
- `GET /api/projections/officers/{badgeNumber}/history` - Get officer status history
- `GET /api/projections/officers?status={status}&rank={rank}&page={page}&size={size}` - List officers with filters

**Vehicles:**
- `GET /api/projections/vehicles/{unitId}` - Get vehicle by unit ID
- `GET /api/projections/vehicles/{unitId}/history` - Get vehicle status history
- `GET /api/projections/vehicles?status={status}&page={page}&size={size}` - List vehicles with filters

**Units:**
- `GET /api/projections/units/{unitId}` - Get unit by unit ID
- `GET /api/projections/units/{unitId}/history` - Get unit status history
- `GET /api/projections/units?status={status}&page={page}&size={size}` - List units with filters

**Persons:**
- `GET /api/projections/persons/{personId}` - Get person by person ID
- `GET /api/projections/persons?page={page}&size={size}` - List persons with pagination

**Locations:**
- `GET /api/projections/locations/{locationId}` - Get location by location ID
- `GET /api/projections/locations?page={page}&size={size}` - List locations with pagination

### Workforce Projection (Port 8083)

**Shifts:**
- `GET /api/projections/shifts/{shiftId}` - Get shift by shift ID
- `GET /api/projections/shifts/{shiftId}/history` - Get shift status history
- `GET /api/projections/shifts?status={status}&shiftType={type}&page={page}&size={size}` - List shifts with filters

**Officer Shifts:**
- `GET /api/projections/officer-shifts/{id}` - Get officer shift by ID
- `GET /api/projections/officer-shifts?shiftId={id}&badgeNumber={badge}&page={page}&size={size}` - List officer shifts with filters

**Shift Changes:**
- `GET /api/projections/shift-changes/{shiftChangeId}` - Get shift change by shift change ID
- `GET /api/projections/shift-changes?shiftId={id}&page={page}&size={size}` - List shift changes with filters

**Note**: Projection services are eventually consistent. Data may lag slightly behind command operations.

## Event Production

All API endpoints produce events to Kafka topics:

- Officer operations → `officer-events` topic
- Vehicle operations → `vehicle-events` topic
- Unit operations → `unit-events` topic
- Person operations → `person-events` topic
- Location operations → `location-events` topic
- Incident operations → `incident-events` topic
- Call operations → `call-events` topic
- Activity operations → `activity-events` topic
- Assignment operations → `assignment-events` topic
- Shift operations → `shift-events` topic
- Dispatch operations → `dispatch-events` topic
- Resource assignment operations → `resource-assignment-events` topic
- Involved party operations → `involved-party-events` topic
- Officer shift operations → `officer-shift-events` topic

## Example Requests

### Register an Officer

```bash
curl -X POST http://localhost:8080/api/officers \
  -H "Content-Type: application/json" \
  -d '{
    "badgeNumber": "12345",
    "firstName": "John",
    "lastName": "Doe",
    "rank": "Officer",
    "email": "john.doe@police.gov",
    "phoneNumber": "555-0100",
    "hireDate": "2020-01-15",
    "status": "Active"
  }'
```

### Report an Incident

```bash
curl -X POST http://localhost:8080/api/incidents \
  -H "Content-Type: application/json" \
  -d '{
    "incidentId": "INC-001",
    "incidentNumber": "2024-001",
    "priority": "High",
    "status": "Reported",
    "reportedTime": "2024-01-15T10:30:00Z",
    "description": "Traffic accident at Main St and Oak Ave",
    "incidentType": "Traffic"
  }'
```

### Dispatch an Incident

```bash
curl -X POST http://localhost:8080/api/incidents/INC-001/dispatch \
  -H "Content-Type: application/json" \
  -d '{
    "dispatchedTime": "2024-01-15T10:35:00Z"
  }'
```

## Testing

All endpoints can be tested using:
- Swagger UI (interactive testing)
- Postman (collection import)
- curl commands (see examples above)
- Any HTTP client

## Notes

- This specification represents the complete API when all increments are implemented
- Some endpoints may not be available until their corresponding increment is completed
- All timestamps use ISO 8601 format (date-time)
- All dates use ISO 8601 format (date)
- Enum values are case-sensitive

