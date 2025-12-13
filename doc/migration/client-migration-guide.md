# Client Migration Guide

## Overview

This guide helps clients migrate from old individual projection services to new consolidated projection services.

## Client Identification

### Primary Client: Edge Service

**Status:** ✅ Already Migrated (Phase 4)

The edge service uses NATS queries (`query.{domain}.exists`, `query.{domain}.get`) to query projections synchronously. This was migrated in Phase 4 and automatically routes to new consolidated projections.

**No Action Required:** Edge service migration is complete.

### Direct REST API Clients

**Status:** No external clients identified in codebase

If you have external clients calling projection REST APIs directly, follow the migration steps below.

## API Endpoint Mapping

### Old Individual Projections → New Consolidated Projections

#### Operational Entities

**Old Endpoints:**
- `http://localhost:8082/api/projections/incidents` (incident-projection)
- `http://localhost:8083/api/projections/calls` (call-projection)
- `http://localhost:8084/api/projections/dispatches` (dispatch-projection)
- `http://localhost:8085/api/projections/activities` (activity-projection)
- `http://localhost:8086/api/projections/assignments` (assignment-projection)

**New Endpoint:**
- `http://localhost:8081/api/projections/*` (operational-projection)

**Mapping:**
- `/api/projections/incidents` → `/api/projections/incidents` (same path, different service)
- `/api/projections/calls` → `/api/projections/calls` (same path, different service)
- `/api/projections/dispatches` → `/api/projections/dispatches` (same path, different service)
- `/api/projections/activities` → `/api/projections/activities` (same path, different service)
- `/api/projections/assignments` → `/api/projections/assignments` (same path, different service)

**Service Port Change:**
- Old: Multiple ports (8082-8086)
- New: Single port 8081 (operational-projection)

#### Resource Entities

**Old Endpoint:**
- `http://localhost:8081/api/projections/officers` (officer-projection)

**New Endpoint:**
- `http://localhost:8082/api/projections/*` (resource-projection)

**Mapping:**
- `/api/projections/officers` → `/api/projections/officers` (same path, different service)
- `/api/projections/vehicles` → `/api/projections/vehicles` (new, resource-projection)
- `/api/projections/units` → `/api/projections/units` (new, resource-projection)
- `/api/projections/persons` → `/api/projections/persons` (new, resource-projection)
- `/api/projections/locations` → `/api/projections/locations` (new, resource-projection)

**Service Port Change:**
- Old: Port 8081 (officer-projection)
- New: Port 8082 (resource-projection)

#### Workforce Entities

**Old Endpoint:**
- None (workforce entities are new)

**New Endpoint:**
- `http://localhost:8083/api/projections/*` (workforce-projection)

**Mapping:**
- `/api/projections/shifts` → `/api/projections/shifts` (new, workforce-projection)
- `/api/projections/officer-shifts` → `/api/projections/officer-shifts` (new, workforce-projection)
- `/api/projections/shift-changes` → `/api/projections/shift-changes` (new, workforce-projection)

**Service Port:**
- New: Port 8083 (workforce-projection)

## Migration Steps

### Step 1: Identify Clients

1. **Inventory Clients:**
   - List all clients calling projection REST APIs
   - Document which endpoints each client uses
   - Document current service URLs/ports

2. **Check Service Discovery:**
   - If using service discovery (Kubernetes, Consul, etc.), update service registrations
   - Update load balancer configurations
   - Update API gateway routes if applicable

### Step 2: Update Client Configurations

#### Update Service URLs

**Before:**
```yaml
# Old configuration
projections:
  incident:
    url: http://incident-projection:8082
  call:
    url: http://call-projection:8083
  officer:
    url: http://officer-projection:8081
```

**After:**
```yaml
# New configuration
projections:
  operational:
    url: http://operational-projection:8081
  resource:
    url: http://resource-projection:8082
  workforce:
    url: http://workforce-projection:8083
```

#### Update API Endpoints

**No Changes Required:** API endpoint paths remain the same. Only service URLs/ports change.

**Example:**
- Old: `http://incident-projection:8082/api/projections/incidents/INC-001`
- New: `http://operational-projection:8081/api/projections/incidents/INC-001`

### Step 3: Test Client Integrations

1. **Update Client Configuration:**
   - Point to new projection services
   - Update service discovery if used

2. **Run Integration Tests:**
   - Test all API endpoints used by client
   - Verify response formats match
   - Check error handling

3. **Monitor:**
   - Monitor client logs for errors
   - Check API response times
   - Verify data correctness

### Step 4: Deploy and Monitor

1. **Deploy Updated Clients:**
   - Deploy clients with new configuration
   - Monitor for errors
   - Check service health

2. **Verify Functionality:**
   - Test all client features
   - Verify data accuracy
   - Check performance

3. **Rollback Plan:**
   - Keep old projection services running during migration
   - Can rollback by reverting client configuration
   - Old projections continue to work

## API Compatibility

### Endpoint Compatibility

**✅ Fully Compatible:** All API endpoints remain the same:
- Path structure unchanged
- Request/response formats unchanged
- Query parameters unchanged
- Error responses unchanged

### Response Format Compatibility

**✅ Fully Compatible:** Response DTOs remain the same:
- Same field names
- Same data types
- Same structure

### Example: Officer Projection

**Old API:**
```http
GET http://officer-projection:8081/api/projections/officers/BADGE-001
```

**New API:**
```http
GET http://resource-projection:8082/api/projections/officers/BADGE-001
```

**Response (unchanged):**
```json
{
  "badgeNumber": "BADGE-001",
  "firstName": "John",
  "lastName": "Doe",
  "rank": "Sergeant",
  "email": "john.doe@police.gov",
  "phoneNumber": "555-0100",
  "hireDate": "2020-01-15",
  "status": "Active",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

## Service Discovery Updates

### Kubernetes

**Old Service:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: incident-projection
spec:
  ports:
    - port: 8082
  selector:
    app: incident-projection
```

**New Service:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: operational-projection
spec:
  ports:
    - port: 8081
  selector:
    app: operational-projection
```

### Load Balancer

**Update load balancer rules:**
- Old: Route `/api/projections/incidents/*` to `incident-projection:8082`
- New: Route `/api/projections/incidents/*` to `operational-projection:8081`

### API Gateway

**Update gateway routes:**
- Old: `incident-projection` service
- New: `operational-projection` service (handles multiple entity types)

## NATS Query Migration

### Edge Service (Already Complete)

**Status:** ✅ Migrated in Phase 4

The edge service uses `ProjectionQueryService` which automatically routes to new consolidated projections via NATS subjects. No client changes needed.

### Other NATS Clients

If you have other services using NATS queries:

**No Changes Required:** NATS subjects remain the same:
- `query.incident.exists` → Routes to operational-projection
- `query.call.exists` → Routes to operational-projection
- `query.officer.exists` → Routes to resource-projection
- etc.

**Routing:** NATS automatically routes to whichever projection subscribes to the subject. During parallel deployment, routing may be non-deterministic. After migration, only new projections subscribe.

## Migration Checklist

- [ ] Identify all clients using projection APIs
- [ ] Document current service URLs/ports
- [ ] Update client configurations
- [ ] Update service discovery (if used)
- [ ] Update load balancer (if used)
- [ ] Update API gateway (if used)
- [ ] Test client integrations
- [ ] Deploy updated clients
- [ ] Monitor for errors
- [ ] Verify functionality
- [ ] Document migration completion

## Troubleshooting

### Issue: Client Cannot Connect

**Symptom:** Client receives connection errors

**Possible Causes:**
- Service URL incorrect
- Port number incorrect
- Service not running
- Network/firewall issues

**Resolution:**
- Verify service URL and port
- Check service health endpoint
- Verify network connectivity
- Check firewall rules

### Issue: 404 Not Found

**Symptom:** Client receives 404 for valid endpoints

**Possible Causes:**
- Wrong service URL
- Endpoint path incorrect
- Entity doesn't exist

**Resolution:**
- Verify endpoint path
- Check service logs
- Verify entity exists in database

### Issue: Response Format Changed

**Symptom:** Client cannot parse response

**Possible Causes:**
- Response format actually changed (unlikely)
- Client parsing logic issue

**Resolution:**
- Verify response format matches documentation
- Check client parsing logic
- Compare old vs new responses

## Next Steps

After client migration:

1. **Validate:** Ensure all clients working correctly
2. **Monitor:** Monitor for any issues
3. **Deprecate:** Proceed with deprecation of old projections (Increment 5.4)
