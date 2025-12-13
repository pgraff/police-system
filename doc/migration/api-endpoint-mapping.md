# API Endpoint Mapping: Old → New Projections

## Overview

This document provides a complete mapping of API endpoints from old individual projections to new consolidated projections.

## Service Ports

### Old Individual Projections

- **officer-projection:** Port 8081 (or random port 0)
- **incident-projection:** Port 8082 (or random port 0)
- **call-projection:** Port 8083 (or random port 0)
- **dispatch-projection:** Port 8084 (or random port 0)
- **activity-projection:** Port 8085 (or random port 0)
- **assignment-projection:** Port 8086 (or random port 0)

### New Consolidated Projections

- **operational-projection:** Port 8081
- **resource-projection:** Port 8082
- **workforce-projection:** Port 8083

## Endpoint Mappings

### Operational Projection (Port 8081)

Handles: incidents, calls, dispatches, activities, assignments, involved parties, resource assignments

#### Incidents

**Old:**
```
GET http://incident-projection:8082/api/projections/incidents/{incidentId}
GET http://incident-projection:8082/api/projections/incidents/{incidentId}/history
GET http://incident-projection:8082/api/projections/incidents?status={status}&priority={priority}&page={page}&size={size}
GET http://incident-projection:8082/api/projections/incidents/{incidentId}/full
```

**New:**
```
GET http://operational-projection:8081/api/projections/incidents/{incidentId}
GET http://operational-projection:8081/api/projections/incidents/{incidentId}/history
GET http://operational-projection:8081/api/projections/incidents?status={status}&priority={priority}&page={page}&size={size}
GET http://operational-projection:8081/api/projections/incidents/{incidentId}/full
```

**Changes:** Service URL only (path unchanged)

#### Calls

**Old:**
```
GET http://call-projection:8083/api/projections/calls/{callId}
GET http://call-projection:8083/api/projections/calls/{callId}/history
GET http://call-projection:8083/api/projections/calls?status={status}&priority={priority}&page={page}&size={size}
```

**New:**
```
GET http://operational-projection:8081/api/projections/calls/{callId}
GET http://operational-projection:8081/api/projections/calls/{callId}/history
GET http://operational-projection:8081/api/projections/calls?status={status}&priority={priority}&page={page}&size={size}
```

**Changes:** Service URL only (path unchanged)

#### Dispatches

**Old:**
```
GET http://dispatch-projection:8084/api/projections/dispatches/{dispatchId}
GET http://dispatch-projection:8084/api/projections/dispatches/{dispatchId}/history
GET http://dispatch-projection:8084/api/projections/dispatches?status={status}&page={page}&size={size}
```

**New:**
```
GET http://operational-projection:8081/api/projections/dispatches/{dispatchId}
GET http://operational-projection:8081/api/projections/dispatches/{dispatchId}/history
GET http://operational-projection:8081/api/projections/dispatches?status={status}&page={page}&size={size}
```

**Changes:** Service URL only (path unchanged)

#### Activities

**Old:**
```
GET http://activity-projection:8085/api/projections/activities/{activityId}
GET http://activity-projection:8085/api/projections/activities/{activityId}/history
GET http://activity-projection:8085/api/projections/activities?status={status}&activityType={type}&page={page}&size={size}
```

**New:**
```
GET http://operational-projection:8081/api/projections/activities/{activityId}
GET http://operational-projection:8081/api/projections/activities/{activityId}/history
GET http://operational-projection:8081/api/projections/activities?status={status}&activityType={type}&page={page}&size={size}
```

**Changes:** Service URL only (path unchanged)

#### Assignments

**Old:**
```
GET http://assignment-projection:8086/api/projections/assignments/{assignmentId}
GET http://assignment-projection:8086/api/projections/assignments/{assignmentId}/history
GET http://assignment-projection:8086/api/projections/assignments?status={status}&page={page}&size={size}
```

**New:**
```
GET http://operational-projection:8081/api/projections/assignments/{assignmentId}
GET http://operational-projection:8081/api/projections/assignments/{assignmentId}/history
GET http://operational-projection:8081/api/projections/assignments?status={status}&page={page}&size={size}
```

**Changes:** Service URL only (path unchanged)

### Resource Projection (Port 8082)

Handles: officers, vehicles, units, persons, locations

#### Officers

**Old:**
```
GET http://officer-projection:8081/api/projections/officers/{badgeNumber}
GET http://officer-projection:8081/api/projections/officers/{badgeNumber}/history
GET http://officer-projection:8081/api/projections/officers?status={status}&rank={rank}&page={page}&size={size}
```

**New:**
```
GET http://resource-projection:8082/api/projections/officers/{badgeNumber}
GET http://resource-projection:8082/api/projections/officers/{badgeNumber}/history
GET http://resource-projection:8082/api/projections/officers?status={status}&rank={rank}&page={page}&size={size}
```

**Changes:** Service URL and port (path unchanged)

#### Vehicles (New)

**New:**
```
GET http://resource-projection:8082/api/projections/vehicles/{unitId}
GET http://resource-projection:8082/api/projections/vehicles/{unitId}/history
GET http://resource-projection:8082/api/projections/vehicles?status={status}&page={page}&size={size}
```

**Note:** Vehicles were not in old projections

#### Units (New)

**New:**
```
GET http://resource-projection:8082/api/projections/units/{unitId}
GET http://resource-projection:8082/api/projections/units/{unitId}/history
GET http://resource-projection:8082/api/projections/units?status={status}&page={page}&size={size}
```

**Note:** Units were not in old projections

#### Persons (New)

**New:**
```
GET http://resource-projection:8082/api/projections/persons/{personId}
GET http://resource-projection:8082/api/projections/persons?page={page}&size={size}
```

**Note:** Persons were not in old projections

#### Locations (New)

**New:**
```
GET http://resource-projection:8082/api/projections/locations/{locationId}
GET http://resource-projection:8082/api/projections/locations?page={page}&size={size}
```

**Note:** Locations were not in old projections

### Workforce Projection (Port 8083)

Handles: shifts, officer shifts, shift changes

#### Shifts (New)

**New:**
```
GET http://workforce-projection:8083/api/projections/shifts/{shiftId}
GET http://workforce-projection:8083/api/projections/shifts/{shiftId}/history
GET http://workforce-projection:8083/api/projections/shifts?status={status}&shiftType={type}&page={page}&size={size}
```

**Note:** Shifts were not in old projections

#### Officer Shifts (New)

**New:**
```
GET http://workforce-projection:8083/api/projections/officer-shifts/{id}
GET http://workforce-projection:8083/api/projections/officer-shifts?shiftId={shiftId}&badgeNumber={badge}&page={page}&size={size}
```

**Note:** Officer shifts were not in old projections

#### Shift Changes (New)

**New:**
```
GET http://workforce-projection:8083/api/projections/shift-changes/{shiftChangeId}
GET http://workforce-projection:8083/api/projections/shift-changes?shiftId={shiftId}&page={page}&size={size}
```

**Note:** Shift changes were not in old projections

## NATS Query Mappings

### No Changes Required

NATS query subjects remain unchanged. Routing happens automatically:

**Operational:**
- `query.incident.exists` → operational-projection
- `query.call.exists` → operational-projection
- `query.dispatch.exists` → operational-projection
- `query.activity.exists` → operational-projection
- `query.assignment.exists` → operational-projection

**Resource:**
- `query.officer.exists` → resource-projection
- `query.vehicle.exists` → resource-projection
- `query.unit.exists` → resource-projection
- `query.person.exists` → resource-projection
- `query.location.exists` → resource-projection

**Workforce:**
- `query.shift.exists` → workforce-projection
- `query.officer-shift.exists` → workforce-projection
- `query.shift-change.exists` → workforce-projection

## Migration Summary

### What Changes

1. **Service URLs:** Update base URLs to new consolidated projection services
2. **Service Ports:** Update ports (see mapping above)
3. **Service Discovery:** Update service registrations if using service discovery

### What Stays the Same

1. **API Paths:** All endpoint paths remain unchanged
2. **Request Formats:** Request formats unchanged
3. **Response Formats:** Response formats unchanged
4. **Query Parameters:** Query parameters unchanged
5. **NATS Subjects:** NATS query subjects unchanged

## Quick Reference

### Service URL Changes

| Entity Type | Old Service | New Service | Port Change |
|------------|------------|-------------|-------------|
| incident | incident-projection | operational-projection | 8082 → 8081 |
| call | call-projection | operational-projection | 8083 → 8081 |
| dispatch | dispatch-projection | operational-projection | 8084 → 8081 |
| activity | activity-projection | operational-projection | 8085 → 8081 |
| assignment | assignment-projection | operational-projection | 8086 → 8081 |
| officer | officer-projection | resource-projection | 8081 → 8082 |
| vehicle | (new) | resource-projection | (new) → 8082 |
| unit | (new) | resource-projection | (new) → 8082 |
| person | (new) | resource-projection | (new) → 8082 |
| location | (new) | resource-projection | (new) → 8082 |
| shift | (new) | workforce-projection | (new) → 8083 |
| officer-shift | (new) | workforce-projection | (new) → 8083 |
| shift-change | (new) | workforce-projection | (new) → 8083 |
