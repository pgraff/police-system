package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.shifts.ChangeShiftStatusRequested;
import com.knowit.policesystem.common.events.shifts.EndShiftRequested;
import com.knowit.policesystem.common.events.shifts.StartShiftRequested;
import com.knowit.policesystem.projection.model.ShiftProjectionEntity;
import com.knowit.policesystem.projection.model.ShiftStatusHistoryEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class ShiftProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public ShiftProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(StartShiftRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        INSERT INTO shift_projection (shift_id, start_time, end_time, shift_type, status, updated_at, event_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (shift_id) DO UPDATE SET
                            start_time = EXCLUDED.start_time,
                            end_time = EXCLUDED.end_time,
                            shift_type = EXCLUDED.shift_type,
                            status = EXCLUDED.status,
                            updated_at = EXCLUDED.updated_at,
                            event_id = EXCLUDED.event_id
                        WHERE shift_projection.event_id IS NULL OR shift_projection.event_id != EXCLUDED.event_id
                        """,
                event.getShiftId(),
                ts(event.getStartTime()),
                ts(event.getEndTime()),
                event.getShiftType(),
                event.getStatus(),
                ts(updatedAt),
                event.getEventId() != null ? event.getEventId().toString() : null);
    }

    public void applyUpdate(EndShiftRequested event, Instant updatedAt) {
        ShiftProjectionEntity current = findByShiftId(event.getShiftId()).orElse(null);
        ShiftProjectionEntity merged = mergeUpdate(current, event, updatedAt);
        upsertMerged(merged, event.getEventId() != null ? event.getEventId().toString() : null);
    }

    public boolean changeStatus(ChangeShiftStatusRequested event, Instant updatedAt) {
        ShiftProjectionEntity current = findByShiftId(event.getShiftId()).orElse(null);
        String currentStatus = current != null ? current.status() : null;
        String newStatus = event.getStatus();

        // Idempotent: only add history when status actually changes
        boolean statusChanged = newStatus != null && !newStatus.equals(currentStatus);

        ShiftProjectionEntity next = mergeStatus(current, event, updatedAt);
        upsertMerged(next, event.getEventId() != null ? event.getEventId().toString() : null);

        if (statusChanged) {
            jdbcTemplate.update("""
                    INSERT INTO shift_status_history (shift_id, status, changed_at, event_id)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (event_id) DO NOTHING
                    """, event.getShiftId(), newStatus, ts(updatedAt), event.getEventId() != null ? event.getEventId().toString() : null);
        }

        return statusChanged;
    }

    public Optional<ShiftProjectionEntity> findByShiftId(String shiftId) {
        List<ShiftProjectionEntity> result = jdbcTemplate.query("""
                SELECT shift_id, start_time, end_time, shift_type, status, updated_at
                FROM shift_projection
                WHERE shift_id = ?
                """, projectionMapper(), shiftId);
        return result.stream().findFirst();
    }

    public List<ShiftStatusHistoryEntry> findHistory(String shiftId) {
        return jdbcTemplate.query("""
                SELECT status, changed_at
                FROM shift_status_history
                WHERE shift_id = ?
                ORDER BY changed_at
                """, historyMapper(), shiftId);
    }

    public List<ShiftProjectionEntity> findAll(String status, String shiftType, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT shift_id, start_time, end_time, shift_type, status, updated_at
                FROM shift_projection
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, status, shiftType);
        sql.append(" ORDER BY shift_id LIMIT ? OFFSET ?");
        Object[] params = filters.withPaging(size, (long) page * size);
        return jdbcTemplate.query(sql.toString(), projectionMapper(), params);
    }

    public long count(String status, String shiftType) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM shift_projection WHERE 1=1");
        QueryFilters filters = new QueryFilters(sql, status, shiftType);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, filters.params());
    }

    private ShiftProjectionEntity mergeUpdate(ShiftProjectionEntity current, EndShiftRequested event, Instant updatedAt) {
        return new ShiftProjectionEntity(
                event.getShiftId(),
                current != null ? current.startTime() : null,
                event.getEndTime(),
                current != null ? current.shiftType() : null,
                current != null ? current.status() : null,
                updatedAt
        );
    }

    private ShiftProjectionEntity mergeStatus(ShiftProjectionEntity current, ChangeShiftStatusRequested event, Instant updatedAt) {
        return new ShiftProjectionEntity(
                event.getShiftId(),
                current != null ? current.startTime() : null,
                current != null ? current.endTime() : null,
                current != null ? current.shiftType() : null,
                event.getStatus(),
                updatedAt
        );
    }

    private void upsertMerged(ShiftProjectionEntity entity, String eventId) {
        jdbcTemplate.update("""
                        INSERT INTO shift_projection (shift_id, start_time, end_time, shift_type, status, updated_at, event_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (shift_id) DO UPDATE SET
                            start_time = COALESCE(EXCLUDED.start_time, shift_projection.start_time),
                            end_time = COALESCE(EXCLUDED.end_time, shift_projection.end_time),
                            shift_type = COALESCE(EXCLUDED.shift_type, shift_projection.shift_type),
                            status = COALESCE(EXCLUDED.status, shift_projection.status),
                            updated_at = EXCLUDED.updated_at,
                            event_id = EXCLUDED.event_id
                        WHERE shift_projection.event_id IS NULL OR shift_projection.event_id != EXCLUDED.event_id
                        """,
                entity.shiftId(),
                ts(entity.startTime()),
                ts(entity.endTime()),
                entity.shiftType(),
                entity.status(),
                ts(entity.updatedAt()),
                eventId != null ? eventId : null);
    }

    private RowMapper<ShiftProjectionEntity> projectionMapper() {
        return (rs, rowNum) -> mapProjection(rs);
    }

    private ShiftProjectionEntity mapProjection(ResultSet rs) throws SQLException {
        return new ShiftProjectionEntity(
                rs.getString("shift_id"),
                rs.getTimestamp("start_time") != null ? rs.getTimestamp("start_time").toInstant() : null,
                rs.getTimestamp("end_time") != null ? rs.getTimestamp("end_time").toInstant() : null,
                rs.getString("shift_type"),
                rs.getString("status"),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private RowMapper<ShiftStatusHistoryEntry> historyMapper() {
        return (rs, rowNum) -> mapHistory(rs);
    }

    private ShiftStatusHistoryEntry mapHistory(ResultSet rs) throws SQLException {
        return new ShiftStatusHistoryEntry(
                rs.getString("status"),
                rs.getTimestamp("changed_at").toInstant()
        );
    }

    private java.sql.Timestamp ts(Instant instant) {
        return instant != null ? java.sql.Timestamp.from(instant) : null;
    }

    private static class QueryFilters {
        private final Object[] params;

        QueryFilters(StringBuilder sql, String status, String shiftType) {
            if (status != null && !status.isBlank()) {
                sql.append(" AND status = ?");
            }
            if (shiftType != null && !shiftType.isBlank()) {
                sql.append(" AND shift_type = ?");
            }
            this.params = buildParams(status, shiftType);
        }

        private Object[] buildParams(String status, String shiftType) {
            if ((status == null || status.isBlank()) && (shiftType == null || shiftType.isBlank())) {
                return new Object[0];
            }
            if (status != null && !status.isBlank() && shiftType != null && !shiftType.isBlank()) {
                return new Object[]{status, shiftType};
            }
            if (status != null && !status.isBlank()) {
                return new Object[]{status};
            }
            return new Object[]{shiftType};
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
