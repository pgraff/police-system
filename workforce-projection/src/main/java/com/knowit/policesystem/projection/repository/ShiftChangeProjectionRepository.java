package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.shifts.RecordShiftChangeRequested;
import com.knowit.policesystem.projection.model.ShiftChangeProjectionEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class ShiftChangeProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public ShiftChangeProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(RecordShiftChangeRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        INSERT INTO shift_change_projection (shift_change_id, shift_id, change_time, change_type, notes, updated_at, event_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (shift_change_id) DO UPDATE SET
                            shift_id = EXCLUDED.shift_id,
                            change_time = EXCLUDED.change_time,
                            change_type = EXCLUDED.change_type,
                            notes = EXCLUDED.notes,
                            updated_at = EXCLUDED.updated_at,
                            event_id = EXCLUDED.event_id
                        WHERE shift_change_projection.event_id IS NULL OR shift_change_projection.event_id != EXCLUDED.event_id
                        """,
                event.getShiftChangeId(),
                event.getShiftId(),
                ts(event.getChangeTime()),
                event.getChangeType(),
                event.getNotes(),
                ts(updatedAt),
                event.getEventId() != null ? event.getEventId().toString() : null);
    }

    public Optional<ShiftChangeProjectionEntity> findByShiftChangeId(String shiftChangeId) {
        List<ShiftChangeProjectionEntity> result = jdbcTemplate.query("""
                SELECT shift_change_id, shift_id, change_time, change_type, notes, updated_at
                FROM shift_change_projection
                WHERE shift_change_id = ?
                """, projectionMapper(), shiftChangeId);
        return result.stream().findFirst();
    }

    public List<ShiftChangeProjectionEntity> findByShiftId(String shiftId) {
        return jdbcTemplate.query("""
                SELECT shift_change_id, shift_id, change_time, change_type, notes, updated_at
                FROM shift_change_projection
                WHERE shift_id = ?
                ORDER BY change_time
                """, projectionMapper(), shiftId);
    }

    public List<ShiftChangeProjectionEntity> findAll(String shiftId, String changeType, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT shift_change_id, shift_id, change_time, change_type, notes, updated_at
                FROM shift_change_projection
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, shiftId, changeType);
        sql.append(" ORDER BY change_time DESC LIMIT ? OFFSET ?");
        Object[] params = filters.withPaging(size, (long) page * size);
        return jdbcTemplate.query(sql.toString(), projectionMapper(), params);
    }

    public long count(String shiftId, String changeType) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM shift_change_projection WHERE 1=1");
        QueryFilters filters = new QueryFilters(sql, shiftId, changeType);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, filters.params());
    }

    private RowMapper<ShiftChangeProjectionEntity> projectionMapper() {
        return (rs, rowNum) -> mapProjection(rs);
    }

    private ShiftChangeProjectionEntity mapProjection(ResultSet rs) throws SQLException {
        return new ShiftChangeProjectionEntity(
                rs.getString("shift_change_id"),
                rs.getString("shift_id"),
                rs.getTimestamp("change_time") != null ? rs.getTimestamp("change_time").toInstant() : null,
                rs.getString("change_type"),
                rs.getString("notes"),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private java.sql.Timestamp ts(Instant instant) {
        return instant != null ? java.sql.Timestamp.from(instant) : null;
    }

    private static class QueryFilters {
        private final Object[] params;

        QueryFilters(StringBuilder sql, String shiftId, String changeType) {
            if (shiftId != null && !shiftId.isBlank()) {
                sql.append(" AND shift_id = ?");
            }
            if (changeType != null && !changeType.isBlank()) {
                sql.append(" AND change_type = ?");
            }
            this.params = buildParams(shiftId, changeType);
        }

        private Object[] buildParams(String shiftId, String changeType) {
            if ((shiftId == null || shiftId.isBlank()) && (changeType == null || changeType.isBlank())) {
                return new Object[0];
            }
            if (shiftId != null && !shiftId.isBlank() && changeType != null && !changeType.isBlank()) {
                return new Object[]{shiftId, changeType};
            }
            if (shiftId != null && !shiftId.isBlank()) {
                return new Object[]{shiftId};
            }
            return new Object[]{changeType};
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
