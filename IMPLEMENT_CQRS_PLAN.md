# IMPLEMENT_CQRS_PLAN

Phased plan to transition to full CQRS with projections. Each increment has scope, deliverables, and test criteria. Status starts as TODO; update as we progress.

## Status Summary

**✅ Completed: Phases 1-4**
- ✅ Phase 1: Projection Foundations (module, schema, infrastructure)
- ✅ Phase 2: Officer Projection (all event handlers, E2E tests)
- ✅ Phase 3: Query APIs (all endpoints, integration tests)
- ✅ Phase 4.9: Idempotency (tested and verified)
- ✅ Phase 4.10: Observability & readiness (health endpoints, metrics, tests)

**⏳ Remaining:**
- ⏳ Phase 5: Expand to other domains (assignment, etc.)
  - ✅ Call Projection (complete)
  - ✅ Dispatch Projection (complete)
  - ✅ Activity Projection (complete)

**Reference Implementation:**
The officer projection (`officer-projection` module) serves as the complete reference implementation. See `/doc/architecture/projection-pattern.md` for the implementation pattern guide.

**Architecture Note:**
Each projection is a separate deployable service (future K8s pod). The `officer-projection` module is the first; future projections (incident, dispatch, call, etc.) will follow the same pattern as separate modules.

## Phase 1 – Projection Foundations
1) Module scaffolding (✅ COMPLETE)  
   - Add `officer-projection` Maven module; parent POM updated.  
   - Add basic Spring Boot app entrypoint.  
   - Test: `mvn -pl officer-projection test` builds/starts.

2) Infrastructure & schema (✅ COMPLETE)  
   - Add Postgres schema: `officer_projection`, `officer_status_history`.  
   - Configure Kafka/NATS clients for officer-projection module.  
   - Test: migration runs in tests; tables exist.

## Phase 2 – Officer Projection (Commands → Read Model)
3) Consume RegisterOfficerRequested (✅ COMPLETE)  
   - Kafka + NATS consumer; upsert officer row.  
   - Test: publish event → row exists with all fields.

4) Consume UpdateOfficerRequested (✅ COMPLETE)  
   - Apply partial updates to projection row.  
   - Test: publish update → fields changed, others preserved.

5) Consume ChangeOfficerStatusRequested + history (✅ COMPLETE)  
   - Update status; insert into status history table.  
   - Test: publish change-status → status updated; history row appended.

6) Officer projection E2E smoke (✅ COMPLETE)  
   - End-to-end from edge REST (register/update/status) → Kafka/NATS → projection Postgres.  
   - Test: targeted smoke using Testcontainers for Kafka/NATS/Postgres.

## Phase 3 – Projection Query APIs
7) Officer query endpoints (✅ COMPLETE)  
   - `GET /api/projections/officers/{badgeNumber}`  
   - `GET /api/projections/officers?status=&rank=&page=&size=`  
   - `GET /api/projections/officers/{badgeNumber}/history`  
   - Test: REST responses match projection DB state.

8) Projection query tests (✅ COMPLETE)  
   - Integration tests against Postgres (Testcontainers).  
   - Verify filters, paging, and history retrieval.

## Phase 4 – Hardening & Ops
9) Idempotency and retries (✅ COMPLETE)  
   - Ensure upserts are idempotent; handle replays.  
   - Test: re-publish same events → no duplication in main table/history.

10) Observability & readiness (✅ COMPLETE)  
   - Added Spring Boot Actuator to all projection modules.  
   - Configured health, readiness, and liveness endpoints.  
   - Added ConsumerMetrics component for tracking consumer errors (example in officer-projection).  
   - Enhanced error logging with event context (topic, partition, offset).  
   - Test: HealthEndpointTest verifies all actuator endpoints return UP status.

## Phase 5 – Expand to Other Domains (Follow-on)
11) Add next domain projections (incident, dispatch, etc.) (IN PROGRESS)  
   - ✅ `incident-projection` - Complete (all event handlers, E2E tests, query APIs)
   - ✅ `call-projection` - Complete (all event handlers, E2E tests, query APIs)
   - ✅ `dispatch-projection` - Complete (all event handlers, E2E tests, query APIs)
   - ✅ `activity-projection` - Complete (all event handlers, E2E tests, query APIs)
   - ⏳ `assignment-projection` - TODO
   - Each is a standalone deployable service (future K8s pod).  
   - Repeat pattern: schema, consumers, queries, tests per domain.  
   - Test: domain-specific smokes + query tests.

## Notes
- All officer events are critical: publish to both Kafka and NATS.  
- Keep increments small; each increment should add tests that fail before code and pass after.  
- Use Testcontainers for Kafka/NATS/Postgres in officer-projection module tests; keep suites targeted (per-increment) to avoid long runs.
- Each projection is a separate deployable service (future K8s pod): `officer-projection`, `incident-projection`, `dispatch-projection`, etc.  
- Maintain status updates here as increments complete.
