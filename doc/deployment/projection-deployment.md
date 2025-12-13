# Projection Deployment Guide

## Overview

This guide covers deployment of the consolidated projection services for parallel operation with existing individual projection services during migration.

## Architecture

### Consolidated Projections

- **operational-projection** (Port 8081)
  - Handles: incidents, calls, dispatches, activities, assignments, involved parties, resource assignments
  - Consumer Group: `operational-projection-service`
  - Database Tables: `incident_projection`, `call_projection`, `dispatch_projection`, `activity_projection`, `assignment_projection`, `involved_party_projection`, `resource_assignment_projection`, and status history tables

- **resource-projection** (Port 8082)
  - Handles: officers, vehicles, units, persons, locations
  - Consumer Group: `resource-projection-service`
  - Database Tables: `officer_projection`, `vehicle_projection`, `unit_projection`, `person_projection`, `location_projection`, and status history tables

- **workforce-projection** (Port 8083)
  - Handles: shifts, officer shifts, shift changes
  - Consumer Group: `workforce-projection-service`
  - Database Tables: `shift_projection`, `officer_shift_projection`, `shift_change_projection`, `shift_status_history`

### Individual Projections (Legacy)

- **officer-projection** (Port 0 - random)
  - Consumer Group: `projection-service` (or `officer-projection-group`)
  - Database Tables: `officer_projection`, `officer_status_history`

- **incident-projection** (Port 0 - random)
  - Consumer Group: `incident-projection-group`
  - Database Tables: `incident_projection`, `incident_status_history`

- **call-projection** (Port 0 - random)
  - Consumer Group: `call-projection-group`
  - Database Tables: `call_projection`, `call_status_history`

- **dispatch-projection** (Port 0 - random)
  - Consumer Group: `dispatch-projection-group`
  - Database Tables: `dispatch_projection`, `dispatch_status_history`

- **activity-projection** (Port 0 - random)
  - Consumer Group: `activity-projection-group`
  - Database Tables: `activity_projection`, `activity_status_history`

- **assignment-projection** (Port 0 - random)
  - Consumer Group: `assignment-projection-group`
  - Database Tables: `assignment_projection`, `assignment_status_history`

## Parallel Deployment Configuration

### Key Configuration Differences

#### Consumer Group IDs

**New Consolidated Projections:**
- `operational-projection-service`
- `resource-projection-service`
- `workforce-projection-service`

**Old Individual Projections:**
- `officer-projection-group` (or `projection-service`)
- `incident-projection-group`
- `call-projection-group`
- `dispatch-projection-group`
- `activity-projection-group`
- `assignment-projection-group`

**Important:** Different consumer group IDs ensure both old and new projections consume the same Kafka topics independently without conflicts.

#### Service Ports

**New Consolidated Projections:**
- operational-projection: `8081`
- resource-projection: `8082`
- workforce-projection: `8083`

**Old Individual Projections:**
- Use random ports (port 0) or specific ports if configured

#### Database Configuration

**Shared Database, Separate Tables:**
- All projections use the same PostgreSQL database (`police`)
- Tables are separated by name (no schema separation needed)
- New consolidated projections use the same table names as old projections for the same entities
- **Note:** During parallel deployment, both old and new projections will write to the same tables. This is safe due to idempotency (event ID tracking), but ensure only one set is actively serving queries.

**Database Connection:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/police
    username: ${PROJECTION_DATASOURCE_USERNAME:postgres}
    password: ${PROJECTION_DATASOURCE_PASSWORD:postgres}
```

#### NATS Configuration

**Both old and new projections subscribe to the same NATS subjects:**
- `query.incident.>`, `query.call.>`, etc. (operational)
- `query.officer.>`, `query.vehicle.>`, etc. (resource)
- `query.shift.>`, `query.officer-shift.>`, etc. (workforce)

**Important:** NATS routes queries to the first responder. During parallel deployment, queries may be handled by either old or new projections (non-deterministic). This is acceptable during migration but should be monitored.

## Deployment Steps

### 1. Prerequisites

- Kafka cluster running and accessible
- NATS cluster running and accessible
- PostgreSQL database running and accessible
- Infrastructure services healthy

### 2. Deploy New Consolidated Projections

#### Build and Package

```bash
# Build all consolidated projections
mvn clean package -pl operational-projection,resource-projection,workforce-projection

# Verify JARs are created
ls -la operational-projection/target/*.jar
ls -la resource-projection/target/*.jar
ls -la workforce-projection/target/*.jar
```

#### Start Services

**operational-projection:**
```bash
java -jar operational-projection/target/operational-projection-*.jar \
  --spring.kafka.consumer.group-id=operational-projection-service \
  --server.port=8081 \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/police
```

**resource-projection:**
```bash
java -jar resource-projection/target/resource-projection-*.jar \
  --spring.kafka.consumer.group-id=resource-projection-service \
  --server.port=8082 \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/police
```

**workforce-projection:**
```bash
java -jar workforce-projection/target/workforce-projection-*.jar \
  --spring.kafka.consumer.group-id=workforce-projection-service \
  --server.port=8083 \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/police
```

### 3. Verify Deployment

#### Health Checks

Check health endpoints for each service:

```bash
# Operational projection
curl http://localhost:8081/actuator/health

# Resource projection
curl http://localhost:8082/actuator/health

# Workforce projection
curl http://localhost:8083/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

#### Kafka Consumer Status

Verify consumers are registered and consuming:

```bash
# Check Kafka consumer groups
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

# Should see:
# - operational-projection-service
# - resource-projection-service
# - workforce-projection-service
# - officer-projection-group (old)
# - incident-projection-group (old)
# - etc.
```

#### NATS Subscriptions

Verify NATS subscriptions:

```bash
# Check NATS connections (via NATS Tower or CLI)
# Should see subscriptions to query.* subjects from both old and new projections
```

### 4. Monitor Parallel Operation

#### Metrics to Monitor

1. **Kafka Consumer Lag**
   - Monitor lag for both old and new consumer groups
   - Ensure both are processing events

2. **Database Activity**
   - Monitor table write activity
   - Verify both projections are writing (idempotent, so safe)

3. **NATS Query Routing**
   - Monitor which projection handles queries
   - May be non-deterministic during parallel deployment

4. **Service Health**
   - Monitor health endpoints for all projections
   - Set up alerts for service failures

5. **API Response Times**
   - Compare response times between old and new projections
   - Monitor for any performance regressions

## Rollback Procedure

If issues are detected during parallel deployment:

1. **Stop New Consolidated Projections**
   ```bash
   # Stop operational-projection
   pkill -f operational-projection
   
   # Stop resource-projection
   pkill -f resource-projection
   
   # Stop workforce-projection
   pkill -f workforce-projection
   ```

2. **Verify Old Projections Still Running**
   - Check health endpoints for old projections
   - Verify they're still consuming from Kafka
   - Verify they're still responding to NATS queries

3. **Investigate Issues**
   - Review logs from new projections
   - Check for configuration errors
   - Verify database connectivity
   - Check Kafka/NATS connectivity

4. **Fix and Redeploy**
   - Fix identified issues
   - Redeploy new projections
   - Monitor closely

## Configuration Reference

### Environment Variables

**Common to All Projections:**
- `PROJECTION_DATASOURCE_URL` - PostgreSQL connection URL
- `PROJECTION_DATASOURCE_USERNAME` - Database username
- `PROJECTION_DATASOURCE_PASSWORD` - Database password
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka broker addresses
- `NATS_URL` - NATS server URL
- `NATS_ENABLED` - Enable/disable NATS (default: true)
- `NATS_QUERY_ENABLED` - Enable/disable NATS queries (default: true)

**Projection-Specific:**
- `PROJECTION_KAFKA_GROUP_ID` - Kafka consumer group ID (must be unique per projection)
- `SERVER_PORT` - HTTP server port (must be unique per projection)

### Example Production Configuration

**operational-projection:**
```yaml
spring:
  kafka:
    consumer:
      group-id: operational-projection-service
server:
  port: 8081
```

**resource-projection:**
```yaml
spring:
  kafka:
    consumer:
      group-id: resource-projection-service
server:
  port: 8082
```

**workforce-projection:**
```yaml
spring:
  kafka:
    consumer:
      group-id: workforce-projection-service
server:
  port: 8083
```

## Troubleshooting

### Issue: Consumer Group Conflicts

**Symptom:** Kafka consumer errors, duplicate processing

**Solution:** Ensure each projection uses a unique consumer group ID

### Issue: Port Conflicts

**Symptom:** Service fails to start, port already in use

**Solution:** Configure unique ports for each projection service

### Issue: Database Table Conflicts

**Symptom:** Data inconsistencies, duplicate writes

**Solution:** During parallel deployment, both projections write to same tables. This is safe due to idempotency, but ensure only one set serves queries. After migration, stop old projections.

### Issue: NATS Query Routing Issues

**Symptom:** Queries handled by wrong projection

**Solution:** NATS routes to first responder. During parallel deployment, this is non-deterministic. After migration, stop old projections to ensure queries go to new ones.

### Issue: High Consumer Lag

**Symptom:** Events not processed in time

**Solution:** 
- Check service health
- Verify Kafka connectivity
- Check database connectivity
- Scale up projection services if needed

## Next Steps

After successful parallel deployment:

1. **Data Validation** (Increment 5.2)
   - Compare data between old and new projections
   - Validate consistency

2. **Client Migration** (Increment 5.3)
   - Migrate clients to new projection APIs
   - Update configurations

3. **Deprecation** (Increment 5.4)
   - Deprecate and remove old projections
   - Clean up code and documentation
