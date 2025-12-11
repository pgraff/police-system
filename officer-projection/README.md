# Officer Projection Module

Officer projection service that consumes officer events from Kafka/NATS, stores projection tables in Postgres, and exposes read-only query endpoints. This is a standalone deployable service (future K8s pod).

## Endpoints
- `GET /api/projections/officers/{badgeNumber}`
- `GET /api/projections/officers/{badgeNumber}/history`
- `GET /api/projections/officers?status=&rank=&page=&size=`

## Configuration
- `PROJECTION_DATASOURCE_URL` / `PROJECTION_DATASOURCE_USERNAME` / `PROJECTION_DATASOURCE_PASSWORD` for Postgres.
- `KAFKA_BOOTSTRAP_SERVERS` for Kafka bootstrap.
- `PROJECTION_KAFKA_GROUP_ID` to override consumer group id.
- `NATS_URL` and `NATS_ENABLED` for NATS consumption (subject `commands.officer.*`).
- Topics use `TopicConfiguration.OFFICER_EVENTS` (`officer-events`).

## Tests
Run projection integration tests with Testcontainers:
```
mvn -pl officer-projection -am test -Dtest=*OfficerProjection* -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false
```
