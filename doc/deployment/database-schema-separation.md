# Database Schema Separation for Parallel Deployment

## Overview

During parallel deployment, both old individual projections and new consolidated projections will run simultaneously. This document describes the database schema separation strategy to avoid conflicts.

## Schema Strategy

### Current Approach: Shared Database, Separate Tables

All projections use the same PostgreSQL database (`police`) but with separate table names. This approach:

- **Pros:**
  - Simple configuration (no schema changes needed)
  - Easy to query across projections if needed
  - Standard PostgreSQL setup

- **Cons:**
  - During parallel deployment, both old and new projections write to the same tables
  - Requires idempotency to prevent duplicate data

### Table Naming Convention

Tables are named by entity type with `_projection` suffix:

- `incident_projection`
- `call_projection`
- `officer_projection`
- `shift_projection`
- etc.

Status history tables use `_status_history` suffix:

- `incident_status_history`
- `officer_status_history`
- etc.

## Parallel Deployment Considerations

### Table Conflicts

**Important:** During parallel deployment, both old and new projections may write to the same tables. This is handled safely through:

1. **Idempotency:** All projections track `event_id` (UUID) to prevent duplicate processing
2. **Upsert Operations:** Use `INSERT ... ON CONFLICT DO UPDATE` to handle concurrent writes
3. **Event ID Tracking:** Each table includes `event_id` column to ensure idempotent processing

### Example: Officer Projection Conflict

**Old Projection:** `officer-projection` writes to `officer_projection` table
**New Projection:** `resource-projection` writes to `officer_projection` table

**Resolution:**
- Both use same table name
- Both track `event_id` in writes
- Upsert operations prevent conflicts
- After migration, stop old projection

### Entity Mapping

#### Operational Projection Tables
- `incident_projection` (conflicts with old `incident-projection`)
- `call_projection` (conflicts with old `call-projection`)
- `dispatch_projection` (conflicts with old `dispatch-projection`)
- `activity_projection` (conflicts with old `activity-projection`)
- `assignment_projection` (conflicts with old `assignment-projection`)
- `involved_party_projection` (new, no conflict)
- `resource_assignment_projection` (new, no conflict)
- Status history tables for each entity

#### Resource Projection Tables
- `officer_projection` (conflicts with old `officer-projection`)
- `vehicle_projection` (new, no conflict)
- `unit_projection` (new, no conflict)
- `person_projection` (new, no conflict)
- `location_projection` (new, no conflict)
- Status history tables for each entity

#### Workforce Projection Tables
- `shift_projection` (new, no conflict)
- `officer_shift_projection` (new, no conflict)
- `shift_change_projection` (new, no conflict)
- `shift_status_history` (new, no conflict)

## Idempotency Mechanism

### Event ID Tracking

All projection tables include an `event_id` column (VARCHAR/UUID) that stores the unique event identifier:

```sql
CREATE TABLE IF NOT EXISTS incident_projection (
    incident_id VARCHAR PRIMARY KEY,
    ...
    event_id VARCHAR NOT NULL,
    ...
);
```

### Upsert Pattern

All repository operations use upsert pattern with event ID checking:

```sql
INSERT INTO incident_projection (incident_id, ..., event_id, ...)
VALUES (?, ..., ?, ...)
ON CONFLICT (incident_id) 
DO UPDATE SET
    ... = COALESCE(EXCLUDED..., incident_projection...),
    event_id = CASE 
        WHEN incident_projection.event_id = EXCLUDED.event_id 
        THEN incident_projection.event_id 
        ELSE EXCLUDED.event_id 
    END
WHERE incident_projection.event_id != EXCLUDED.event_id
   OR incident_projection.event_id IS NULL;
```

This ensures:
- Same event processed twice → no change (idempotent)
- Different events for same entity → update (latest wins)
- Concurrent writes → safe (PostgreSQL handles locking)

## Migration Strategy

### Phase 1: Parallel Deployment (Current)

- Both old and new projections run
- Both write to same tables
- Idempotency ensures no data corruption
- Monitor for any issues

### Phase 2: Data Validation

- Compare data between old and new projections
- Verify consistency
- Document any differences

### Phase 3: Client Migration

- Migrate clients to new projections
- Old projections still running (backup)

### Phase 4: Deprecation

- Stop old projections
- Only new projections write to tables
- Remove old projection code

## Verification Queries

### Check Table Existence

```sql
-- List all projection tables
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
  AND table_name LIKE '%_projection'
ORDER BY table_name;
```

### Check Event ID Coverage

```sql
-- Verify all records have event_id
SELECT 
    COUNT(*) as total_records,
    COUNT(event_id) as records_with_event_id,
    COUNT(*) - COUNT(event_id) as missing_event_ids
FROM incident_projection;
```

### Check for Duplicate Events

```sql
-- Find duplicate event IDs (should be none)
SELECT event_id, COUNT(*) as count
FROM incident_projection
GROUP BY event_id
HAVING COUNT(*) > 1;
```

## Recommendations

1. **Monitor Event Processing:**
   - Track which projection processed which events
   - Monitor for duplicate processing (shouldn't happen due to idempotency)

2. **Database Performance:**
   - Monitor write performance during parallel deployment
   - Consider indexing on `event_id` if needed

3. **Migration Timeline:**
   - Keep parallel deployment period short (1-2 weeks)
   - Validate data quickly
   - Migrate clients promptly
   - Deprecate old projections as soon as safe

4. **Rollback Plan:**
   - If issues occur, stop new projections
   - Old projections continue operating
   - No data loss (both write to same tables)

## Alternative: Schema Separation (Future Consideration)

If table conflicts become an issue, consider using PostgreSQL schemas:

```sql
CREATE SCHEMA IF NOT EXISTS operational_projection;
CREATE SCHEMA IF NOT EXISTS resource_projection;
CREATE SCHEMA IF NOT EXISTS workforce_projection;
CREATE SCHEMA IF NOT EXISTS legacy_projections;
```

This would require:
- Schema-qualified table names in queries
- Migration scripts to move data
- More complex configuration

**Current approach (shared tables) is recommended** due to simplicity and idempotency guarantees.
