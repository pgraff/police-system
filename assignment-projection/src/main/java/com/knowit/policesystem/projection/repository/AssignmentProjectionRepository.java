package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.assignments.ChangeAssignmentStatusRequested;
import com.knowit.policesystem.common.events.assignments.CompleteAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.CreateAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.LinkAssignmentToDispatchRequested;
import com.knowit.policesystem.common.events.resourceassignment.AssignResourceRequested;
import com.knowit.policesystem.projection.model.AssignmentProjectionEntity;
import com.knowit.policesystem.projection.model.AssignmentResourceEntry;
import com.knowit.policesystem.projection.model.AssignmentStatusHistoryEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AssignmentProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public AssignmentProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(CreateAssignmentRequested event, Instant now) {
        jdbcTemplate.update("""
                        INSERT INTO assignment_projection (assignment_id, assigned_time, assignment_type, status, incident_id, call_id, dispatch_id, completed_time, created_at, updated_at, event_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, NULL, COALESCE(?, NOW()), ?, ?)
                        ON CONFLICT (assignment_id) DO UPDATE SET
                            assigned_time = EXCLUDED.assigned_time,
                            assignment_type = EXCLUDED.assignment_type,
                            status = EXCLUDED.status,
                            incident_id = EXCLUDED.incident_id,
                            call_id = EXCLUDED.call_id,
                            dispatch_id = COALESCE(EXCLUDED.dispatch_id, assignment_projection.dispatch_id),
                            updated_at = EXCLUDED.updated_at,
                            event_id = EXCLUDED.event_id
                        """,
                event.getAssignmentId(),
                ts(event.getAssignedTime()),
                event.getAssignmentType(),
                event.getStatus(),
                event.getIncidentId(),
                event.getCallId(),
                null,
                ts(now),
                ts(now),
                event.getEventId());

        insertHistoryIfNew(event.getAssignmentId(), event.getStatus(), now, event.getEventId());
    }

    public void changeStatus(ChangeAssignmentStatusRequested event, Instant now) {
        jdbcTemplate.update("""
                        UPDATE assignment_projection
                        SET status = ?, updated_at = ?, event_id = ?
                        WHERE assignment_id = ?
                        """,
                event.getStatus(),
                ts(now),
                event.getEventId(),
                event.getAssignmentId());

        insertHistoryIfNew(event.getAssignmentId(), event.getStatus(), now, event.getEventId());
    }

    public void complete(CompleteAssignmentRequested event, Instant now) {
        jdbcTemplate.update("""
                        UPDATE assignment_projection
                        SET status = ?, completed_time = ?, updated_at = ?, event_id = ?
                        WHERE assignment_id = ?
                        """,
                "Completed",
                ts(event.getCompletedTime()),
                ts(now),
                event.getEventId(),
                event.getAssignmentId());

        insertHistoryIfNew(event.getAssignmentId(), "Completed", now, event.getEventId());
    }

    public void linkDispatch(LinkAssignmentToDispatchRequested event, Instant now) {
        jdbcTemplate.update("""
                        UPDATE assignment_projection
                        SET dispatch_id = ?, updated_at = ?, event_id = ?
                        WHERE assignment_id = ?
                        """,
                event.getDispatchId(),
                ts(now),
                event.getEventId(),
                event.getAssignmentId());
    }

    public void upsertResource(AssignResourceRequested event, Instant now) {
        jdbcTemplate.update("""
                        INSERT INTO assignment_resource (assignment_id, resource_id, resource_type, role_type, status, start_time, updated_at, event_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (assignment_id, resource_id, resource_type) DO UPDATE SET
                            role_type = COALESCE(EXCLUDED.role_type, assignment_resource.role_type),
                            status = COALESCE(EXCLUDED.status, assignment_resource.status),
                            start_time = COALESCE(EXCLUDED.start_time, assignment_resource.start_time),
                            updated_at = EXCLUDED.updated_at,
                            event_id = EXCLUDED.event_id
                        """,
                event.getAssignmentId(),
                event.getResourceId(),
                event.getResourceType(),
                event.getRoleType(),
                event.getStatus(),
                ts(event.getStartTime()),
                ts(now),
                event.getEventId());
    }

    public Optional<AssignmentProjectionEntity> findByAssignmentId(String assignmentId) {
        List<AssignmentProjectionEntity> result = jdbcTemplate.query("""
                SELECT assignment_id, assigned_time, assignment_type, status, incident_id, call_id, dispatch_id, completed_time, created_at, updated_at
                FROM assignment_projection
                WHERE assignment_id = ?
                """, projectionMapper(), assignmentId);
        return result.stream().findFirst();
    }

    public List<AssignmentStatusHistoryEntry> findHistory(String assignmentId) {
        return jdbcTemplate.query("""
                SELECT id, assignment_id, status, changed_at
                FROM assignment_status_history
                WHERE assignment_id = ?
                ORDER BY changed_at
                """, historyMapper(), assignmentId);
    }

    public List<AssignmentResourceEntry> findResources(String assignmentId) {
        return jdbcTemplate.query("""
                SELECT id, assignment_id, resource_id, resource_type, role_type, status, start_time, updated_at
                FROM assignment_resource
                WHERE assignment_id = ?
                ORDER BY updated_at DESC
                """, resourceMapper(), assignmentId);
    }

    public List<AssignmentProjectionEntity> findAll(String status, String assignmentType, String dispatchId,
                                                    String incidentId, String callId, String resourceId,
                                                    int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT ap.assignment_id, ap.assigned_time, ap.assignment_type, ap.status, ap.incident_id, ap.call_id, ap.dispatch_id,
                       ap.completed_time, ap.created_at, ap.updated_at
                FROM assignment_projection ap
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, status, assignmentType, dispatchId, incidentId, callId, resourceId);
        sql.append(" ORDER BY ap.assigned_time DESC NULLS LAST, ap.assignment_id LIMIT ? OFFSET ?");
        Object[] params = filters.withPaging(size, (long) page * size);
        return jdbcTemplate.query(sql.toString(), projectionMapper(), params);
    }

    public long count(String status, String assignmentType, String dispatchId,
                      String incidentId, String callId, String resourceId) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM assignment_projection ap
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, status, assignmentType, dispatchId, incidentId, callId, resourceId);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, filters.params());
    }

    private void insertHistoryIfNew(String assignmentId, String status, Instant when, String eventId) {
        jdbcTemplate.update("""
                INSERT INTO assignment_status_history (assignment_id, status, changed_at, event_id)
                SELECT ?, ?, ?, ?
                WHERE NOT EXISTS (SELECT 1 FROM assignment_status_history WHERE event_id = ?)
                """, assignmentId, status, ts(when), eventId, eventId);
    }

    private RowMapper<AssignmentProjectionEntity> projectionMapper() {
        return this::mapProjection;
    }

    private AssignmentProjectionEntity mapProjection(ResultSet rs) throws SQLException {
        return new AssignmentProjectionEntity(
                rs.getString("assignment_id"),
                tsInstant(rs, "assigned_time"),
                rs.getString("assignment_type"),
                rs.getString("status"),
                rs.getString("incident_id"),
                rs.getString("call_id"),
                rs.getString("dispatch_id"),
                tsInstant(rs, "completed_time"),
                tsInstant(rs, "created_at"),
                tsInstant(rs, "updated_at")
        );
    }

    private RowMapper<AssignmentStatusHistoryEntry> historyMapper() {
        return this::mapHistory;
    }

    private AssignmentStatusHistoryEntry mapHistory(ResultSet rs) throws SQLException {
        return new AssignmentStatusHistoryEntry(
                rs.getLong("id"),
                rs.getString("assignment_id"),
                rs.getString("status"),
                tsInstant(rs, "changed_at")
        );
    }

    private RowMapper<AssignmentResourceEntry> resourceMapper() {
        return this::mapResource;
    }

    private AssignmentResourceEntry mapResource(ResultSet rs) throws SQLException {
        return new AssignmentResourceEntry(
                rs.getLong("id"),
                rs.getString("assignment_id"),
                rs.getString("resource_id"),
                rs.getString("resource_type"),
                rs.getString("role_type"),
                rs.getString("status"),
                tsInstant(rs, "start_time"),
                tsInstant(rs, "updated_at")
        );
    }

    private java.sql.Timestamp ts(Instant instant) {
        return instant != null ? java.sql.Timestamp.from(instant) : null;
    }

    private Instant tsInstant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column) != null ? rs.getTimestamp(column).toInstant() : null;
    }

    private static class QueryFilters {
        private final List<Object> params = new ArrayList<>();

        QueryFilters(StringBuilder sql, String status, String assignmentType, String dispatchId,
                     String incidentId, String callId, String resourceId) {
            if (status != null) {
                sql.append(" AND ap.status = ?");
                params.add(status);
            }
            if (assignmentType != null) {
                sql.append(" AND ap.assignment_type = ?");
                params.add(assignmentType);
            }
            if (dispatchId != null) {
                sql.append(" AND ap.dispatch_id = ?");
                params.add(dispatchId);
            }
            if (incidentId != null) {
                sql.append(" AND ap.incident_id = ?");
                params.add(incidentId);
            }
            if (callId != null) {
                sql.append(" AND ap.call_id = ?");
                params.add(callId);
            }
            if (resourceId != null) {
                sql.append("""
                        AND EXISTS (
                            SELECT 1 FROM assignment_resource ar
                            WHERE ar.assignment_id = ap.assignment_id
                              AND ar.resource_id = ?
                        )
                        """);
                params.add(resourceId);
            }
        }

        Object[] withPaging(int size, long offset) {
            params.add(size);
            params.add(offset);
            return params.toArray();
        }

        Object[] params() {
            return params.toArray();
        }
    }
}
