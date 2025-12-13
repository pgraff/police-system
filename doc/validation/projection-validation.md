# Projection Data Validation Guide

## Overview

This guide covers validation strategies for comparing data between old individual projections and new consolidated projections during parallel deployment.

## Validation Strategy

### Goals

1. **Data Consistency:** Ensure new projections contain the same data as old projections
2. **Completeness:** Verify all entities are projected correctly
3. **Accuracy:** Validate field-level data matches
4. **Relationships:** Verify foreign key relationships are maintained
5. **Status History:** Ensure status history is correctly tracked

### Approach

During parallel deployment, both old and new projections:
- Consume the same Kafka topics
- Write to the same database tables (idempotent)
- Subscribe to the same NATS subjects

**Key Insight:** Since both write to the same tables, we validate:
1. Both projections are processing events correctly
2. Data in tables is consistent and complete
3. No data loss or corruption occurred

## Validation Scripts

### 1. Basic Validation Script

**File:** `scripts/validate-projections.sh`

**Purpose:** Basic validation of projection data

**Usage:**
```bash
# Set environment variables
export PROJECTION_DATASOURCE_HOST=localhost
export PROJECTION_DATASOURCE_PORT=5432
export PROJECTION_DATASOURCE_DB=police
export PROJECTION_DATASOURCE_USERNAME=postgres
export PROJECTION_DATASOURCE_PASSWORD=postgres

# Run validation
./scripts/validate-projections.sh
```

**What it checks:**
- Entity counts for all projection tables
- Status history counts
- Event ID coverage (all records have event_id)
- Duplicate event IDs (should be none)
- Data consistency (null checks, timestamp checks)

### 2. Comparison Script

**File:** `scripts/compare-projections.sh`

**Purpose:** Compare data between old and new projections

**Usage:**
```bash
# Compare all entities
./scripts/compare-projections.sh

# Compare specific entity
./scripts/compare-projections.sh incident INC-001
```

**What it does:**
- Compares entity counts
- Compares status history
- Can compare via REST APIs (if both projections running)
- Generates comparison reports

## Validation Checks

### 1. Entity Count Validation

**Check:** All entities exist in projection tables

**SQL Query:**
```sql
SELECT 
    'incident' as entity, COUNT(*) as count 
FROM incident_projection
UNION ALL
SELECT 'call', COUNT(*) FROM call_projection
UNION ALL
SELECT 'officer', COUNT(*) FROM officer_projection
-- etc.
ORDER BY entity;
```

**Expected Result:** All entities have records (count > 0)

### 2. Event ID Validation

**Check:** All records have event_id (required for idempotency)

**SQL Query:**
```sql
SELECT 
    COUNT(*) as total,
    COUNT(event_id) as with_event_id,
    COUNT(*) - COUNT(event_id) as missing_event_id
FROM incident_projection;
```

**Expected Result:** `missing_event_id = 0`

### 3. Duplicate Event ID Check

**Check:** No duplicate event IDs (should be unique per entity)

**SQL Query:**
```sql
SELECT event_id, COUNT(*) as count
FROM incident_projection
WHERE event_id IS NOT NULL
GROUP BY event_id
HAVING COUNT(*) > 1;
```

**Expected Result:** No rows returned (no duplicates)

### 4. Status History Validation

**Check:** Status history exists for entities with status changes

**SQL Query:**
```sql
-- Compare entity count vs history count
SELECT 
    (SELECT COUNT(*) FROM incident_projection) as entities,
    (SELECT COUNT(*) FROM incident_status_history) as history_entries;
```

**Expected Result:** History entries >= entities (some entities may have multiple status changes)

### 5. Timestamp Validation

**Check:** All records have updated_at timestamp

**SQL Query:**
```sql
SELECT COUNT(*) as missing_timestamps
FROM incident_projection
WHERE updated_at IS NULL;
```

**Expected Result:** `missing_timestamps = 0`

### 6. Relationship Validation

**Check:** Foreign key relationships are maintained

**SQL Query:**
```sql
-- Check assignments linked to incidents/calls/dispatches
SELECT 
    COUNT(*) as total_assignments,
    COUNT(incident_id) as linked_to_incident,
    COUNT(call_id) as linked_to_call,
    COUNT(dispatch_id) as linked_to_dispatch
FROM assignment_projection;
```

**Expected Result:** Relationships are properly maintained

## Automated Validation

### Scheduled Validation Job

Create a cron job or scheduled task to run validation regularly:

```bash
# Run validation daily at 2 AM
0 2 * * * /path/to/scripts/validate-projections.sh >> /var/log/projection-validation.log 2>&1
```

### Continuous Validation

For continuous validation during parallel deployment:

```bash
# Run every 5 minutes
*/5 * * * * /path/to/scripts/validate-projections.sh
```

### Alert on Discrepancies

Add alerting to validation script:

```bash
# In validate-projections.sh, add:
if [ "$missing_event_id" -gt 0 ]; then
    # Send alert (email, Slack, etc.)
    echo "ALERT: Missing event IDs detected" | mail -s "Projection Validation Alert" admin@example.com
fi
```

## Validation Reports

### Report Format

Validation reports should include:

1. **Summary:**
   - Total entities validated
   - Pass/fail counts
   - Overall status

2. **Entity Details:**
   - Count per entity
   - Status history counts
   - Event ID coverage
   - Data quality metrics

3. **Issues:**
   - Missing event IDs
   - Duplicate event IDs
   - Missing timestamps
   - Relationship issues

4. **Trends:**
   - Comparison with previous validation
   - Growth trends
   - Data quality trends

### Example Report

```
Projection Validation Report
Generated: 2024-01-15 10:30:00

=== Summary ===
Total Entities: 10
Passed: 9
Failed: 1
Overall Status: WARNING

=== Entity Details ===
incident: 1,234 records ✓
  - Status history: 2,456 entries ✓
  - Event IDs: 100% coverage ✓
  - Timestamps: All present ✓

officer: 567 records ✓
  - Status history: 890 entries ✓
  - Event IDs: 100% coverage ✓
  - Timestamps: All present ✓

call: 0 records ⚠
  - WARNING: No records found

=== Issues ===
- call: No records found (may be expected if no calls processed yet)

=== Trends ===
- Incident count increased by 50 since last validation
- Officer count stable
```

## Validation Dashboard

### Recommended Metrics

1. **Entity Counts:**
   - Current count per entity
   - Growth rate
   - Comparison with previous period

2. **Data Quality:**
   - Event ID coverage percentage
   - Missing timestamp count
   - Duplicate event ID count

3. **Status History:**
   - History entries per entity
   - Average status changes per entity

4. **Validation Status:**
   - Last validation time
   - Validation pass/fail status
   - Issues detected

### Dashboard Implementation

Use tools like:
- Grafana with PostgreSQL data source
- Custom web dashboard
- Kibana/Elasticsearch (if logging to ES)

## Troubleshooting

### Issue: Missing Event IDs

**Symptom:** Some records don't have event_id

**Possible Causes:**
- Old data before event_id tracking was added
- Data migration issue
- Event processing error

**Resolution:**
- Check if records are from before event_id tracking
- Verify event processing is working
- Consider backfilling event_ids if needed

### Issue: Duplicate Event IDs

**Symptom:** Same event_id appears multiple times

**Possible Causes:**
- Event processed multiple times
- Idempotency not working correctly

**Resolution:**
- Check event processing logs
- Verify idempotency logic in repositories
- Review Kafka consumer configuration

### Issue: Missing Status History

**Symptom:** Entities exist but no status history

**Possible Causes:**
- Entities created but status never changed
- Status history processing error

**Resolution:**
- Check if entities have initial status
- Verify status history processing
- Review status change events

## Next Steps

After validation:

1. **Fix Issues:** Address any validation failures
2. **Document Differences:** Note any expected differences
3. **Monitor Trends:** Track validation metrics over time
4. **Proceed with Migration:** Once validation passes, proceed to client migration
