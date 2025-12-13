# Projection Monitoring Guide

## Overview

This guide covers monitoring strategies for both old individual projections and new consolidated projections during parallel deployment.

## Health Check Endpoints

### Consolidated Projections

All consolidated projections expose Spring Boot Actuator health endpoints:

**operational-projection (Port 8081):**
- Health: `http://localhost:8081/actuator/health`
- Readiness: `http://localhost:8081/actuator/health/readiness`
- Liveness: `http://localhost:8081/actuator/health/liveness`
- Info: `http://localhost:8081/actuator/info`

**resource-projection (Port 8082):**
- Health: `http://localhost:8082/actuator/health`
- Readiness: `http://localhost:8082/actuator/health/readiness`
- Liveness: `http://localhost:8082/actuator/health/liveness`
- Info: `http://localhost:8082/actuator/info`

**workforce-projection (Port 8083):**
- Health: `http://localhost:8083/actuator/health`
- Readiness: `http://localhost:8083/actuator/health/readiness`
- Liveness: `http://localhost:8083/actuator/health/liveness`
- Info: `http://localhost:8083/actuator/info`

### Health Check Response

Expected response when healthy:
```json
{
  "status": "UP"
}
```

When unhealthy:
```json
{
  "status": "DOWN",
  "components": {
    "db": {
      "status": "DOWN",
      "details": {
        "error": "Connection refused"
      }
    }
  }
}
```

## Key Metrics to Monitor

### 1. Service Health

**What to Monitor:**
- Health endpoint status (UP/DOWN)
- Service uptime
- Response times

**How to Monitor:**
```bash
# Check health status
curl -s http://localhost:8081/actuator/health | jq .status

# Monitor continuously
watch -n 5 'curl -s http://localhost:8081/actuator/health | jq .status'
```

**Alert Thresholds:**
- Alert if status is DOWN
- Alert if response time > 1 second
- Alert if service unavailable for > 30 seconds

### 2. Kafka Consumer Lag

**What to Monitor:**
- Consumer group lag for each projection
- Processing rate (events/second)
- Consumer group status

**How to Monitor:**
```bash
# List consumer groups
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

# Check lag for operational-projection
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group operational-projection-service \
  --describe

# Check lag for old projections
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group officer-projection-group \
  --describe
```

**Alert Thresholds:**
- Alert if lag > 1000 messages
- Alert if lag increasing continuously
- Alert if consumer group not active

### 3. Database Connectivity

**What to Monitor:**
- Database connection pool status
- Query response times
- Connection errors

**How to Monitor:**
```sql
-- Check active connections
SELECT count(*) 
FROM pg_stat_activity 
WHERE datname = 'police';

-- Check table sizes
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename LIKE '%_projection'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

**Alert Thresholds:**
- Alert if connection pool exhausted
- Alert if query time > 5 seconds
- Alert if connection errors > 10/minute

### 4. NATS Connectivity

**What to Monitor:**
- NATS connection status
- Subscription status
- Query response times

**How to Monitor:**
```bash
# Check NATS server info
curl http://localhost:8222/varz

# Check connections
curl http://localhost:8222/connz

# Check subscriptions (via NATS CLI)
nats sub list
```

**Alert Thresholds:**
- Alert if NATS connection lost
- Alert if subscriptions not active
- Alert if query timeout > 2 seconds

### 5. API Response Times

**What to Monitor:**
- REST API endpoint response times
- NATS query response times
- Error rates

**How to Monitor:**
```bash
# Test API response time
time curl -s http://localhost:8081/api/projections/incidents/INC-001

# Monitor with Apache Bench
ab -n 100 -c 10 http://localhost:8081/actuator/health
```

**Alert Thresholds:**
- Alert if p95 response time > 500ms
- Alert if error rate > 1%
- Alert if timeout rate > 0.1%

### 6. Event Processing Rate

**What to Monitor:**
- Events processed per second
- Events per consumer group
- Processing delays

**How to Monitor:**
```bash
# Check consumer group metrics
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group operational-projection-service \
  --describe \
  | grep -E "LAG|CURRENT-OFFSET"
```

**Alert Thresholds:**
- Alert if processing rate drops > 50%
- Alert if processing stops
- Alert if lag increases continuously

## Monitoring Dashboard

### Recommended Metrics Dashboard

Create a dashboard with the following panels:

1. **Service Health Status**
   - Health status for all projections (old and new)
   - Uptime percentage
   - Service availability

2. **Kafka Consumer Metrics**
   - Consumer lag per group
   - Processing rate per group
   - Topic consumption rate

3. **Database Metrics**
   - Connection pool usage
   - Query performance
   - Table sizes

4. **API Performance**
   - Response times (p50, p95, p99)
   - Request rate
   - Error rate

5. **Event Processing**
   - Events processed per second
   - Processing delay
   - Backlog size

### Example Prometheus Queries (if using Prometheus)

```promql
# Service uptime
up{job="operational-projection"}

# Kafka consumer lag
kafka_consumer_lag_sum{group="operational-projection-service"}

# API response time
http_request_duration_seconds{endpoint="/api/projections/incidents"}

# Error rate
rate(http_requests_total{status=~"5.."}[5m])
```

## Logging

### Key Log Messages to Monitor

1. **Startup Messages**
   - Service started successfully
   - Kafka consumer connected
   - NATS connected
   - Database connected

2. **Error Messages**
   - Kafka consumer errors
   - NATS connection errors
   - Database connection errors
   - Event processing errors

3. **Performance Warnings**
   - Slow query warnings
   - High consumer lag warnings
   - Connection pool exhaustion

### Log Aggregation

Use centralized logging (e.g., ELK stack, Loki) to:
- Aggregate logs from all projections
- Search across services
- Create alerts based on log patterns
- Track error trends

## Alerting Rules

### Critical Alerts (Immediate Action Required)

1. **Service Down**
   - Any projection service health check fails
   - Action: Check service logs, restart if needed

2. **Database Connection Lost**
   - Database connectivity errors
   - Action: Check database status, network connectivity

3. **Kafka Consumer Stopped**
   - Consumer group not processing
   - Action: Check Kafka connectivity, consumer logs

### Warning Alerts (Investigate)

1. **High Consumer Lag**
   - Lag > 1000 messages
   - Action: Check processing rate, scale if needed

2. **Slow API Responses**
   - p95 response time > 500ms
   - Action: Check database performance, optimize queries

3. **High Error Rate**
   - Error rate > 1%
   - Action: Check error logs, identify root cause

## Monitoring Tools

### Recommended Tools

1. **Health Checks:** Spring Boot Actuator
2. **Metrics:** Micrometer + Prometheus
3. **Logging:** Logback + ELK/Loki
4. **Kafka Monitoring:** Kafka UI, kafka-consumer-groups
5. **NATS Monitoring:** NATS Tower, NATS CLI
6. **Database Monitoring:** pgAdmin, PostgreSQL stats

### Setup Instructions

1. **Enable Actuator Metrics:**
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,metrics,prometheus
   ```

2. **Configure Prometheus:**
   - Add Prometheus endpoint
   - Configure scraping
   - Set up dashboards

3. **Configure Logging:**
   - Set up log aggregation
   - Configure log levels
   - Set up log forwarding

## Troubleshooting

### Service Not Starting

1. Check logs for startup errors
2. Verify port availability
3. Check database connectivity
4. Verify Kafka/NATS connectivity

### High Consumer Lag

1. Check service health
2. Verify Kafka connectivity
3. Check database performance
4. Consider scaling up services

### API Timeouts

1. Check database query performance
2. Verify connection pool settings
3. Check for database locks
4. Optimize slow queries

## Next Steps

After monitoring is set up:

1. **Baseline Metrics:** Establish baseline metrics during parallel deployment
2. **Compare Performance:** Compare old vs new projection performance
3. **Optimize:** Identify and fix performance issues
4. **Validate:** Ensure new projections meet performance requirements
