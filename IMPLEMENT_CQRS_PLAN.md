# IMPLEMENT_CQRS_PLAN

Phased plan to transition to full CQRS with projections. Each increment has scope, deliverables, and test criteria. Status starts as TODO; update as we progress.

## Phase 1 – Projection Foundations
1) Module scaffolding (TODO)  
   - Add `projection` Maven module; parent POM updated.  
   - Add basic Spring Boot app entrypoint.  
   - Test: `mvn -pl projection test` builds/starts.

2) Infrastructure & schema (TODO)  
   - Add Postgres schema: `officer_projection`, `officer_status_history`.  
   - Configure Kafka/NATS clients for projection module.  
   - Test: migration runs in tests; tables exist.

## Phase 2 – Officer Projection (Commands → Read Model)
3) Consume RegisterOfficerRequested (TODO)  
   - Kafka + NATS consumer; upsert officer row.  
   - Test: publish event → row exists with all fields.

4) Consume UpdateOfficerRequested (TODO)  
   - Apply partial updates to projection row.  
   - Test: publish update → fields changed, others preserved.

5) Consume ChangeOfficerStatusRequested + history (TODO)  
   - Update status; insert into status history table.  
   - Test: publish change-status → status updated; history row appended.

6) Officer projection E2E smoke (TODO)  
   - End-to-end from edge REST (register/update/status) → Kafka/NATS → projection Postgres.  
   - Test: targeted smoke using Testcontainers for Kafka/NATS/Postgres.

## Phase 3 – Projection Query APIs
7) Officer query endpoints (TODO)  
   - `GET /api/projections/officers/{badgeNumber}`  
   - `GET /api/projections/officers?status=&rank=&page=&size=`  
   - `GET /api/projections/officers/{badgeNumber}/history`  
   - Test: REST responses match projection DB state.

8) Projection query tests (TODO)  
   - Integration tests against Postgres (Testcontainers).  
   - Verify filters, paging, and history retrieval.

## Phase 4 – Hardening & Ops
9) Idempotency and retries (TODO)  
   - Ensure upserts are idempotent; handle replays.  
   - Test: re-publish same events → no duplication in main table/history.

10) Observability & readiness (TODO)  
   - Health/readiness endpoints; basic metrics/logging for projection consumer lag/errors.  
   - Test: health endpoints up; log on failure; metric exposure stubs in place.

## Phase 5 – Expand to Other Domains (Follow-on)
11) Add next domain projections (incident, dispatch, etc.) (TODO)  
   - Repeat pattern: schema, consumers, queries, tests per domain.  
   - Test: domain-specific smokes + query tests.

## Notes
- All officer events are critical: publish to both Kafka and NATS.  
- Keep increments small; each increment should add tests that fail before code and pass after.  
- Use Testcontainers for Kafka/NATS/Postgres in projection module tests; keep suites targeted (per-increment) to avoid long runs.  
- Maintain status updates here as increments complete.
