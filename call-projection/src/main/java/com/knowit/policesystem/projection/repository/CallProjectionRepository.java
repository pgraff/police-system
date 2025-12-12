package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.calls.ChangeCallStatusRequested;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.common.events.calls.UpdateCallRequested;
import com.knowit.policesystem.projection.model.CallProjectionEntity;
import com.knowit.policesystem.projection.model.CallStatusHistoryEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class CallProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public CallProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(ReceiveCallRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        INSERT INTO call_projection (call_id, call_number, priority, status, received_time, description, call_type, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (call_id) DO UPDATE SET
                            call_number = EXCLUDED.call_number,
                            priority = EXCLUDED.priority,
                            status = EXCLUDED.status,
                            received_time = EXCLUDED.received_time,
                            description = EXCLUDED.description,
                            call_type = EXCLUDED.call_type,
                            updated_at = EXCLUDED.updated_at
                        """,
                event.getCallId(),
                event.getCallNumber(),
                event.getPriority(),
                event.getStatus(),
                ts(event.getReceivedTime()),
                event.getDescription(),
                event.getCallType(),
                ts(updatedAt));
    }

    public void applyUpdate(UpdateCallRequested event, Instant updatedAt) {
        CallProjectionEntity current = findByCallId(event.getCallId()).orElse(null);
        CallProjectionEntity merged = mergeUpdate(current, event, updatedAt);
        upsertMerged(merged);
    }

    public boolean changeStatus(ChangeCallStatusRequested event, Instant updatedAt) {
        CallProjectionEntity current = findByCallId(event.getCallId()).orElse(null);
        String currentStatus = current != null ? current.status() : null;
        String newStatus = event.getStatus();

        // Idempotent: only add history when status actually changes
        boolean statusChanged = newStatus != null && !newStatus.equals(currentStatus);

        CallProjectionEntity next = mergeStatus(current, event, updatedAt);
        upsertMerged(next);

        if (statusChanged) {
            jdbcTemplate.update("""
                    INSERT INTO call_status_history (call_id, status, changed_at)
                    VALUES (?, ?, ?)
                    """, event.getCallId(), newStatus, ts(updatedAt));
        }

        return statusChanged;
    }

    public void updateDispatchTime(String callId, Instant dispatchedTime, Instant updatedAt) {
        jdbcTemplate.update("""
                        UPDATE call_projection
                        SET dispatched_time = ?, updated_at = ?
                        WHERE call_id = ?
                        """,
                ts(dispatchedTime),
                ts(updatedAt),
                callId);
    }

    public void updateArrivedTime(String callId, Instant arrivedTime, Instant updatedAt) {
        jdbcTemplate.update("""
                        UPDATE call_projection
                        SET arrived_time = ?, updated_at = ?
                        WHERE call_id = ?
                        """,
                ts(arrivedTime),
                ts(updatedAt),
                callId);
    }

    public void updateClearedTime(String callId, Instant clearedTime, Instant updatedAt) {
        jdbcTemplate.update("""
                        UPDATE call_projection
                        SET cleared_time = ?, updated_at = ?
                        WHERE call_id = ?
                        """,
                ts(clearedTime),
                ts(updatedAt),
                callId);
    }

    public Optional<CallProjectionEntity> findByCallId(String callId) {
        List<CallProjectionEntity> result = jdbcTemplate.query("""
                SELECT call_id, call_number, priority, status, received_time, dispatched_time, arrived_time, cleared_time, description, call_type, updated_at
                FROM call_projection
                WHERE call_id = ?
                """, projectionMapper(), callId);
        return result.stream().findFirst();
    }

    public List<CallStatusHistoryEntry> findHistory(String callId) {
        return jdbcTemplate.query("""
                SELECT id, call_id, status, changed_at
                FROM call_status_history
                WHERE call_id = ?
                ORDER BY changed_at
                """, historyMapper(), callId);
    }

    public List<CallProjectionEntity> findAll(String status, String priority, String callType, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT call_id, call_number, priority, status, received_time, dispatched_time, arrived_time, cleared_time, description, call_type, updated_at
                FROM call_projection
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, status, priority, callType);
        sql.append(" ORDER BY call_id LIMIT ? OFFSET ?");
        Object[] params = filters.withPaging(size, (long) page * size);
        return jdbcTemplate.query(sql.toString(), projectionMapper(), params);
    }

    public long count(String status, String priority, String callType) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM call_projection WHERE 1=1");
        QueryFilters filters = new QueryFilters(sql, status, priority, callType);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, filters.params());
    }

    private CallProjectionEntity mergeUpdate(CallProjectionEntity current, UpdateCallRequested event, Instant updatedAt) {
        return new CallProjectionEntity(
                event.getCallId(),
                current != null ? current.callNumber() : null,
                event.getPriority() != null ? event.getPriority() : current != null ? current.priority() : null,
                current != null ? current.status() : null,
                current != null ? current.receivedTime() : null,
                current != null ? current.dispatchedTime() : null,
                current != null ? current.arrivedTime() : null,
                current != null ? current.clearedTime() : null,
                event.getDescription() != null ? event.getDescription() : current != null ? current.description() : null,
                event.getCallType() != null ? event.getCallType() : current != null ? current.callType() : null,
                updatedAt
        );
    }

    private CallProjectionEntity mergeStatus(CallProjectionEntity current, ChangeCallStatusRequested event, Instant updatedAt) {
        return new CallProjectionEntity(
                event.getCallId(),
                current != null ? current.callNumber() : null,
                current != null ? current.priority() : null,
                event.getStatus(),
                current != null ? current.receivedTime() : null,
                current != null ? current.dispatchedTime() : null,
                current != null ? current.arrivedTime() : null,
                current != null ? current.clearedTime() : null,
                current != null ? current.description() : null,
                current != null ? current.callType() : null,
                updatedAt
        );
    }

    private void upsertMerged(CallProjectionEntity entity) {
        jdbcTemplate.update("""
                        INSERT INTO call_projection (call_id, call_number, priority, status, received_time, dispatched_time, arrived_time, cleared_time, description, call_type, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (call_id) DO UPDATE SET
                            call_number = COALESCE(EXCLUDED.call_number, call_projection.call_number),
                            priority = COALESCE(EXCLUDED.priority, call_projection.priority),
                            status = COALESCE(EXCLUDED.status, call_projection.status),
                            received_time = COALESCE(EXCLUDED.received_time, call_projection.received_time),
                            dispatched_time = COALESCE(EXCLUDED.dispatched_time, call_projection.dispatched_time),
                            arrived_time = COALESCE(EXCLUDED.arrived_time, call_projection.arrived_time),
                            cleared_time = COALESCE(EXCLUDED.cleared_time, call_projection.cleared_time),
                            description = COALESCE(EXCLUDED.description, call_projection.description),
                            call_type = COALESCE(EXCLUDED.call_type, call_projection.call_type),
                            updated_at = EXCLUDED.updated_at
                        """,
                entity.callId(),
                entity.callNumber(),
                entity.priority(),
                entity.status(),
                ts(entity.receivedTime()),
                ts(entity.dispatchedTime()),
                ts(entity.arrivedTime()),
                ts(entity.clearedTime()),
                entity.description(),
                entity.callType(),
                ts(entity.updatedAt()));
    }

    private RowMapper<CallProjectionEntity> projectionMapper() {
        return (rs, rowNum) -> mapProjection(rs);
    }

    private CallProjectionEntity mapProjection(ResultSet rs) throws SQLException {
        return new CallProjectionEntity(
                rs.getString("call_id"),
                rs.getString("call_number"),
                rs.getString("priority"),
                rs.getString("status"),
                rs.getTimestamp("received_time") != null ? rs.getTimestamp("received_time").toInstant() : null,
                rs.getTimestamp("dispatched_time") != null ? rs.getTimestamp("dispatched_time").toInstant() : null,
                rs.getTimestamp("arrived_time") != null ? rs.getTimestamp("arrived_time").toInstant() : null,
                rs.getTimestamp("cleared_time") != null ? rs.getTimestamp("cleared_time").toInstant() : null,
                rs.getString("description"),
                rs.getString("call_type"),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private RowMapper<CallStatusHistoryEntry> historyMapper() {
        return (rs, rowNum) -> mapHistory(rs);
    }

    private CallStatusHistoryEntry mapHistory(ResultSet rs) throws SQLException {
        return new CallStatusHistoryEntry(
                rs.getLong("id"),
                rs.getString("call_id"),
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

        QueryFilters(StringBuilder sql, String status, String priority, String callType) {
            if (status != null && !status.isBlank()) {
                sql.append(" AND status = ?");
            }
            if (priority != null && !priority.isBlank()) {
                sql.append(" AND priority = ?");
            }
            if (callType != null && !callType.isBlank()) {
                sql.append(" AND call_type = ?");
            }
            this.params = buildParams(status, priority, callType);
        }

        private Object[] buildParams(String status, String priority, String callType) {
            int count = 0;
            if (status != null && !status.isBlank()) count++;
            if (priority != null && !priority.isBlank()) count++;
            if (callType != null && !callType.isBlank()) count++;
            
            if (count == 0) {
                return new Object[0];
            }
            
            Object[] result = new Object[count];
            int idx = 0;
            if (status != null && !status.isBlank()) {
                result[idx++] = status;
            }
            if (priority != null && !priority.isBlank()) {
                result[idx++] = priority;
            }
            if (callType != null && !callType.isBlank()) {
                result[idx++] = callType;
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

