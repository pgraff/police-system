package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.assignments.ChangeAssignmentStatusRequested;
import com.knowit.policesystem.common.events.assignments.CompleteAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.CreateAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.LinkAssignmentToDispatchRequested;
import com.knowit.policesystem.projection.model.AssignmentProjectionEntity;
import com.knowit.policesystem.projection.model.AssignmentStatusHistoryEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class AssignmentProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public AssignmentProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(CreateAssignmentRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        INSERT INTO assignment_projection (assignment_id, assigned_time, assignment_type, status, incident_id, call_id, dispatch_id, completed_time, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, NULL, COALESCE(?, NOW()), ?)
                        ON CONFLICT (assignment_id) DO UPDATE SET
                            assigned_time = EXCLUDED.assigned_time,
                            assignment_type = EXCLUDED.assignment_type,
                            status = EXCLUDED.status,
                            incident_id = EXCLUDED.incident_id,
                            call_id = EXCLUDED.call_id,
                            dispatch_id = COALESCE(EXCLUDED.dispatch_id, assignment_projection.dispatch_id),
                            updated_at = EXCLUDED.updated_at
                        """,
                event.getAssignmentId(),
                ts(event.getAssignedTime()),
                event.getAssignmentType(),
                event.getStatus(),
                event.getIncidentId(),
                event.getCallId(),
                null,
                ts(updatedAt),
                ts(updatedAt));
    }

    public boolean changeStatus(ChangeAssignmentStatusRequested event, Instant updatedAt) {
        AssignmentProjectionEntity current = findByAssignmentId(event.getAssignmentId()).orElse(null);
        String currentStatus = current != null ? current.status() : null;
        String newStatus = event.getStatus();

        // Idempotent: only add history when status actually changes
        boolean statusChanged = newStatus != null && !newStatus.equals(currentStatus);

        jdbcTemplate.update("""
                        UPDATE assignment_projection
                        SET status = ?, updated_at = ?
                        WHERE assignment_id = ?
                        """,
                newStatus,
                ts(updatedAt),
                event.getAssignmentId());

        if (statusChanged) {
            jdbcTemplate.update("""
                    INSERT INTO assignment_status_history (assignment_id, status, changed_at)
                    VALUES (?, ?, ?)
                    """, event.getAssignmentId(), newStatus, ts(updatedAt));
        }

        return statusChanged;
    }

    public void complete(CompleteAssignmentRequested event, Instant updatedAt) {
        // Get current status before updating
        AssignmentProjectionEntity current = findByAssignmentId(event.getAssignmentId()).orElse(null);
        String currentStatus = current != null ? current.status() : null;
        boolean statusChanged = !"Completed".equals(currentStatus);

        jdbcTemplate.update("""
                        UPDATE assignment_projection
                        SET status = ?, completed_time = ?, updated_at = ?
                        WHERE assignment_id = ?
                        """,
                "Completed",
                ts(event.getCompletedTime()),
                ts(updatedAt),
                event.getAssignmentId());

        // Add history entry for completion if status changed
        if (statusChanged) {
            jdbcTemplate.update("""
                    INSERT INTO assignment_status_history (assignment_id, status, changed_at)
                    VALUES (?, ?, ?)
                    """, event.getAssignmentId(), "Completed", ts(updatedAt));
        }
    }

    public void linkDispatch(LinkAssignmentToDispatchRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        UPDATE assignment_projection
                        SET dispatch_id = ?, updated_at = ?
                        WHERE assignment_id = ?
                        """,
                event.getDispatchId(),
                ts(updatedAt),
                event.getAssignmentId());
    }

    public Optional<AssignmentProjectionEntity> findByAssignmentId(String assignmentId) {
        List<AssignmentProjectionEntity> result = jdbcTemplate.query("""
                SELECT assignment_id, assigned_time, assignment_type, status, incident_id, call_id, dispatch_id, completed_time, created_at, updated_at
                FROM assignment_projection
                WHERE assignment_id = ?
                """, projectionMapper(), assignmentId);
        return result.stream().findFirst();
    }

    public List<AssignmentProjectionEntity> findByIncidentId(String incidentId) {
        return jdbcTemplate.query("""
                SELECT assignment_id, assigned_time, assignment_type, status, incident_id, call_id, dispatch_id, completed_time, created_at, updated_at
                FROM assignment_projection
                WHERE incident_id = ?
                ORDER BY assigned_time DESC NULLS LAST
                """, projectionMapper(), incidentId);
    }

    public List<AssignmentProjectionEntity> findByCallId(String callId) {
        return jdbcTemplate.query("""
                SELECT assignment_id, assigned_time, assignment_type, status, incident_id, call_id, dispatch_id, completed_time, created_at, updated_at
                FROM assignment_projection
                WHERE call_id = ?
                ORDER BY assigned_time DESC NULLS LAST
                """, projectionMapper(), callId);
    }

    public List<AssignmentProjectionEntity> findByDispatchId(String dispatchId) {
        return jdbcTemplate.query("""
                SELECT assignment_id, assigned_time, assignment_type, status, incident_id, call_id, dispatch_id, completed_time, created_at, updated_at
                FROM assignment_projection
                WHERE dispatch_id = ?
                ORDER BY assigned_time DESC NULLS LAST
                """, projectionMapper(), dispatchId);
    }

    public List<AssignmentStatusHistoryEntry> findHistory(String assignmentId) {
        return jdbcTemplate.query("""
                SELECT id, assignment_id, status, changed_at
                FROM assignment_status_history
                WHERE assignment_id = ?
                ORDER BY changed_at
                """, historyMapper(), assignmentId);
    }

    public List<AssignmentProjectionEntity> findAll(String status, String assignmentType, String dispatchId,
                                                     String incidentId, String callId, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT assignment_id, assigned_time, assignment_type, status, incident_id, call_id, dispatch_id, completed_time, created_at, updated_at
                FROM assignment_projection
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, status, assignmentType, dispatchId, incidentId, callId);
        sql.append(" ORDER BY assigned_time DESC NULLS LAST, assignment_id LIMIT ? OFFSET ?");
        Object[] params = filters.withPaging(size, (long) page * size);
        return jdbcTemplate.query(sql.toString(), projectionMapper(), params);
    }

    public long count(String status, String assignmentType, String dispatchId,
                      String incidentId, String callId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM assignment_projection WHERE 1=1");
        QueryFilters filters = new QueryFilters(sql, status, assignmentType, dispatchId, incidentId, callId);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, filters.params());
    }

    private RowMapper<AssignmentProjectionEntity> projectionMapper() {
        return (rs, rowNum) -> mapProjection(rs);
    }

    private AssignmentProjectionEntity mapProjection(ResultSet rs) throws SQLException {
        return new AssignmentProjectionEntity(
                rs.getString("assignment_id"),
                rs.getTimestamp("assigned_time") != null ? rs.getTimestamp("assigned_time").toInstant() : null,
                rs.getString("assignment_type"),
                rs.getString("status"),
                rs.getString("incident_id"),
                rs.getString("call_id"),
                rs.getString("dispatch_id"),
                rs.getTimestamp("completed_time") != null ? rs.getTimestamp("completed_time").toInstant() : null,
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null,
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private RowMapper<AssignmentStatusHistoryEntry> historyMapper() {
        return (rs, rowNum) -> mapHistory(rs);
    }

    private AssignmentStatusHistoryEntry mapHistory(ResultSet rs) throws SQLException {
        return new AssignmentStatusHistoryEntry(
                rs.getLong("id"),
                rs.getString("assignment_id"),
                rs.getString("status"),
                rs.getTimestamp("changed_at").toInstant()
        );
    }

    private java.sql.Timestamp ts(Instant instant) {
        return instant != null ? java.sql.Timestamp.from(instant) : null;
    }

    /**
     * Helper to build filter SQL and parameter arrays deterministically.
     */
    private static class QueryFilters {
        private final Object[] params;

        QueryFilters(StringBuilder sql, String status, String assignmentType, String dispatchId,
                     String incidentId, String callId) {
            if (status != null && !status.isBlank()) {
                sql.append(" AND status = ?");
            }
            if (assignmentType != null && !assignmentType.isBlank()) {
                sql.append(" AND assignment_type = ?");
            }
            if (dispatchId != null && !dispatchId.isBlank()) {
                sql.append(" AND dispatch_id = ?");
            }
            if (incidentId != null && !incidentId.isBlank()) {
                sql.append(" AND incident_id = ?");
            }
            if (callId != null && !callId.isBlank()) {
                sql.append(" AND call_id = ?");
            }
            this.params = buildParams(status, assignmentType, dispatchId, incidentId, callId);
        }

        private Object[] buildParams(String status, String assignmentType, String dispatchId,
                                     String incidentId, String callId) {
            int count = 0;
            if (status != null && !status.isBlank()) count++;
            if (assignmentType != null && !assignmentType.isBlank()) count++;
            if (dispatchId != null && !dispatchId.isBlank()) count++;
            if (incidentId != null && !incidentId.isBlank()) count++;
            if (callId != null && !callId.isBlank()) count++;
            
            if (count == 0) {
                return new Object[0];
            }
            
            Object[] result = new Object[count];
            int idx = 0;
            if (status != null && !status.isBlank()) {
                result[idx++] = status;
            }
            if (assignmentType != null && !assignmentType.isBlank()) {
                result[idx++] = assignmentType;
            }
            if (dispatchId != null && !dispatchId.isBlank()) {
                result[idx++] = dispatchId;
            }
            if (incidentId != null && !incidentId.isBlank()) {
                result[idx++] = incidentId;
            }
            if (callId != null && !callId.isBlank()) {
                result[idx++] = callId;
            }
            return result;
        }

        Object[] params() {
            return params;
        }

        Object[] withPaging(int limit, long offset) {
            Object[] pagingParams = new Object[params.length + 2];
            System.arraycopy(params, 0, pagingParams, 0, params.length);
            pagingParams[params.length] = limit;
            pagingParams[params.length + 1] = offset;
            return pagingParams;
        }
    }
}
