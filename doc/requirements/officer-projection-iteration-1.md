# Officer Projection – Iteration 1 (Requirements)

## Goal
Deliver a standalone `projection` Spring Boot module that consumes officer events from Kafka/NATS, projects them into Postgres, and exposes read-only HTTP endpoints for querying the officer read model and status history.

## Scope
- New Maven module: `projection` (packaged as runnable jar; future K8s pod).
- Event inputs (from `TopicConfiguration.OFFICER_EVENTS` and NATS subject derived from `EventClassification.generateNatsSubject`):
  - `RegisterOfficerRequested`
  - `UpdateOfficerRequested`
  - `ChangeOfficerStatusRequested`
- Persistence:
  - `officer_projection(badge_number PK, first_name, last_name, rank, email, phone_number, hire_date, status, updated_at)`
  - `officer_status_history(id PK, badge_number FK, status, changed_at)`
- Outputs: HTTP query endpoints (no command handling, no event emission).

## API Requirements
- Base path: `/api/projections/officers`
- `GET /{badgeNumber}` → 200 with projection row; 404 if missing.
- `GET /{badgeNumber}/history` → 200 with ordered status history; 404 if badge not present in projection.
- `GET /` (optional) → supports `status`, `rank`, `page`, `size` filters with reasonable defaults and total count in response.
- Responses are JSON; include `updatedAt` (ISO-8601) on projection row and `changedAt` on history items.

## Event Handling Requirements
- Kafka consumer subscribes to `TopicConfiguration.OFFICER_EVENTS`.
- NATS subscriber listens on subject from `EventClassification.generateNatsSubject(event)`; safe against duplicate processing (prefer Kafka as source of truth).
- Register/Update:
  - Upsert projection row keyed by `badge_number`.
  - For `UpdateOfficerRequested`, only non-null fields overwrite existing values; preserve others.
  - `updated_at` set to current timestamp on every mutation.
- Change status:
  - Update `status` + `updated_at` on projection row.
  - Append to `officer_status_history` when status actually changes (idempotent on replays).
- Idempotency:
  - Upserts are conflict-safe on PK.
  - History only records when new status differs from last stored status for that badge.

## Acceptance Criteria
- After `RegisterOfficerRequested`, projection row exists with all provided fields and `status` set.
- After `UpdateOfficerRequested`, only provided fields change; others unchanged; `updated_at` refreshed.
- After `ChangeOfficerStatusRequested`, projection `status` matches event and a history row is appended once per distinct change.
- Duplicate delivery of any event does not corrupt data (row intact; no duplicate history when status unchanged).
- Query endpoints return projection rows/history reflecting DB state; 404 for unknown badge.
- Kafka/NATS consumers configurable via YAML (bootstrap servers, group id, client id, NATS url, enable flags).
- Module runs with Postgres datasource properties provided via env/YAML.

## Test Criteria (Step 1)
- Integration tests using Testcontainers for Kafka, NATS, and Postgres:
  - Publish register → row exists with all fields.
  - Register + update → partial fields updated, others preserved.
  - Status change → projection status updated and history row appended; repeat same status → no extra history row.
  - Query endpoints return expected payloads for projection and history; filters paginate correctly (happy-path smoke acceptable for iteration 1).
  - NATS receives at least one critical event (e.g., register) and projection handles Kafka-driven consumption without double-processing.
- Tests initially fail before implementation and pass after.

## Out of Scope
- Additional domain projections (incident, dispatch, etc.).
- Advanced observability/metrics beyond basic logging.
- Authentication/authorization on projection APIs.
