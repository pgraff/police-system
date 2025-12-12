package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.dispatches.ChangeDispatchStatusRequested;
import com.knowit.policesystem.common.events.dispatches.CreateDispatchRequested;
import com.knowit.policesystem.projection.model.DispatchProjectionEntity;
import com.knowit.policesystem.projection.model.DispatchStatusHistoryEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class DispatchProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public DispatchProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(CreateDispatchRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        INSERT INTO dispatch_projection (dispatch_id, dispatch_time, dispatch_type, status, updated_at)
                        VALUES (?, ?, ?, ?, ?)
                        ON CONFLICT (dispatch_id) DO UPDATE SET
                            dispatch_time = EXCLUDED.dispatch_time,
                            dispatch_type = EXCLUDED.dispatch_type,
                            status = EXCLUDED.status,
                            updated_at = EXCLUDED.updated_at
                        """,
                event.getDispatchId(),
                ts(event.getDispatchTime()),
                event.getDispatchType(),
                event.getStatus(),
                ts(updatedAt));
    }

    public boolean changeStatus(ChangeDispatchStatusRequested event, Instant updatedAt) {
        DispatchProjectionEntity current = findByDispatchId(event.getDispatchId()).orElse(null);
        String currentStatus = current != null ? current.status() : null;
        String newStatus = event.getStatus();

        // Idempotent: only add history when status actually changes
        boolean statusChanged = newStatus != null && !newStatus.equals(currentStatus);

        DispatchProjectionEntity next = mergeStatus(current, event, updatedAt);
        upsertMerged(next);

        if (statusChanged) {
            jdbcTemplate.update("""
                    INSERT INTO dispatch_status_history (dispatch_id, status, changed_at)
                    VALUES (?, ?, ?)
                    """, event.getDispatchId(), newStatus, ts(updatedAt));
        }

        return statusChanged;
    }

    public Optional<DispatchProjectionEntity> findByDispatchId(String dispatchId) {
        List<DispatchProjectionEntity> result = jdbcTemplate.query("""
                SELECT dispatch_id, dispatch_time, dispatch_type, status, updated_at
                FROM dispatch_projection
                WHERE dispatch_id = ?
                """, projectionMapper(), dispatchId);
        return result.stream().findFirst();
    }

    public List<DispatchStatusHistoryEntry> findHistory(String dispatchId) {
        return jdbcTemplate.query("""
                SELECT id, dispatch_id, status, changed_at
                FROM dispatch_status_history
                WHERE dispatch_id = ?
                ORDER BY changed_at
                """, historyMapper(), dispatchId);
    }

    public List<DispatchProjectionEntity> findAll(String status, String dispatchType, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT dispatch_id, dispatch_time, dispatch_type, status, updated_at
                FROM dispatch_projection
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, status, dispatchType);
        sql.append(" ORDER BY dispatch_id LIMIT ? OFFSET ?");
        Object[] params = filters.withPaging(size, (long) page * size);
        return jdbcTemplate.query(sql.toString(), projectionMapper(), params);
    }

    public long count(String status, String dispatchType) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM dispatch_projection WHERE 1=1");
        QueryFilters filters = new QueryFilters(sql, status, dispatchType);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, filters.params());
    }

    private DispatchProjectionEntity mergeStatus(DispatchProjectionEntity current, ChangeDispatchStatusRequested event, Instant updatedAt) {
        return new DispatchProjectionEntity(
                event.getDispatchId(),
                current != null ? current.dispatchTime() : null,
                current != null ? current.dispatchType() : null,
                event.getStatus(),
                updatedAt
        );
    }

    private void upsertMerged(DispatchProjectionEntity entity) {
        jdbcTemplate.update("""
                        INSERT INTO dispatch_projection (dispatch_id, dispatch_time, dispatch_type, status, updated_at)
                        VALUES (?, ?, ?, ?, ?)
                        ON CONFLICT (dispatch_id) DO UPDATE SET
                            dispatch_time = COALESCE(EXCLUDED.dispatch_time, dispatch_projection.dispatch_time),
                            dispatch_type = COALESCE(EXCLUDED.dispatch_type, dispatch_projection.dispatch_type),
                            status = COALESCE(EXCLUDED.status, dispatch_projection.status),
                            updated_at = EXCLUDED.updated_at
                        """,
                entity.dispatchId(),
                ts(entity.dispatchTime()),
                entity.dispatchType(),
                entity.status(),
                ts(entity.updatedAt()));
    }

    private RowMapper<DispatchProjectionEntity> projectionMapper() {
        return (rs, rowNum) -> mapProjection(rs);
    }

    private DispatchProjectionEntity mapProjection(ResultSet rs) throws SQLException {
        return new DispatchProjectionEntity(
                rs.getString("dispatch_id"),
                rs.getTimestamp("dispatch_time") != null ? rs.getTimestamp("dispatch_time").toInstant() : null,
                rs.getString("dispatch_type"),
                rs.getString("status"),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private RowMapper<DispatchStatusHistoryEntry> historyMapper() {
        return (rs, rowNum) -> mapHistory(rs);
    }

    private DispatchStatusHistoryEntry mapHistory(ResultSet rs) throws SQLException {
        return new DispatchStatusHistoryEntry(
                rs.getLong("id"),
                rs.getString("dispatch_id"),
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

        QueryFilters(StringBuilder sql, String status, String dispatchType) {
            if (status != null && !status.isBlank()) {
                sql.append(" AND status = ?");
            }
            if (dispatchType != null && !dispatchType.isBlank()) {
                sql.append(" AND dispatch_type = ?");
            }
            this.params = buildParams(status, dispatchType);
        }

        private Object[] buildParams(String status, String dispatchType) {
            int count = 0;
            if (status != null && !status.isBlank()) count++;
            if (dispatchType != null && !dispatchType.isBlank()) count++;
            
            if (count == 0) {
                return new Object[0];
            }
            
            Object[] result = new Object[count];
            int idx = 0;
            if (status != null && !status.isBlank()) {
                result[idx++] = status;
            }
            if (dispatchType != null && !dispatchType.isBlank()) {
                result[idx++] = dispatchType;
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

