package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.officershifts.CheckInOfficerRequested;
import com.knowit.policesystem.common.events.officershifts.CheckOutOfficerRequested;
import com.knowit.policesystem.common.events.officershifts.UpdateOfficerShiftRequested;
import com.knowit.policesystem.projection.model.OfficerShiftProjectionEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class OfficerShiftProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public OfficerShiftProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long upsert(CheckInOfficerRequested event, Instant updatedAt) {
        // Try to find existing officer shift for this shift + badge combination
        Optional<OfficerShiftProjectionEntity> existing = findByShiftIdAndBadgeNumber(
                event.getShiftId(), event.getBadgeNumber());
        
        if (existing.isPresent()) {
            // Update existing
            OfficerShiftProjectionEntity merged = mergeCheckIn(existing.get(), event, updatedAt);
            updateMerged(merged, event.getEventId() != null ? event.getEventId().toString() : null);
            return existing.get().id();
        } else {
            // Insert new
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO officer_shift_projection (shift_id, badge_number, check_in_time, shift_role_type, updated_at, event_id)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, event.getShiftId());
                ps.setString(2, event.getBadgeNumber());
                ps.setTimestamp(3, ts(event.getCheckInTime()));
                ps.setString(4, event.getShiftRoleType());
                ps.setTimestamp(5, ts(updatedAt));
                ps.setString(6, event.getEventId() != null ? event.getEventId().toString() : null);
                return ps;
            }, keyHolder);
            return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
        }
    }

    public void applyUpdate(CheckOutOfficerRequested event, Instant updatedAt) {
        Optional<OfficerShiftProjectionEntity> existing = findByShiftIdAndBadgeNumber(
                event.getShiftId(), event.getBadgeNumber());
        if (existing.isPresent()) {
            OfficerShiftProjectionEntity merged = mergeCheckOut(existing.get(), event, updatedAt);
            updateMerged(merged, event.getEventId() != null ? event.getEventId().toString() : null);
        }
    }

    public void applyUpdate(UpdateOfficerShiftRequested event, Instant updatedAt) {
        Optional<OfficerShiftProjectionEntity> existing = findByShiftIdAndBadgeNumber(
                event.getShiftId(), event.getBadgeNumber());
        if (existing.isPresent()) {
            OfficerShiftProjectionEntity merged = mergeRoleUpdate(existing.get(), event, updatedAt);
            updateMerged(merged, event.getEventId() != null ? event.getEventId().toString() : null);
        }
    }

    public Optional<OfficerShiftProjectionEntity> findById(Long id) {
        List<OfficerShiftProjectionEntity> result = jdbcTemplate.query("""
                SELECT id, shift_id, badge_number, check_in_time, check_out_time, shift_role_type, updated_at
                FROM officer_shift_projection
                WHERE id = ?
                """, projectionMapper(), id);
        return result.stream().findFirst();
    }

    public Optional<OfficerShiftProjectionEntity> findByShiftIdAndBadgeNumber(String shiftId, String badgeNumber) {
        List<OfficerShiftProjectionEntity> result = jdbcTemplate.query("""
                SELECT id, shift_id, badge_number, check_in_time, check_out_time, shift_role_type, updated_at
                FROM officer_shift_projection
                WHERE shift_id = ? AND badge_number = ?
                ORDER BY check_in_time DESC NULLS LAST
                LIMIT 1
                """, projectionMapper(), shiftId, badgeNumber);
        return result.stream().findFirst();
    }

    public List<OfficerShiftProjectionEntity> findByShiftId(String shiftId) {
        return jdbcTemplate.query("""
                SELECT id, shift_id, badge_number, check_in_time, check_out_time, shift_role_type, updated_at
                FROM officer_shift_projection
                WHERE shift_id = ?
                ORDER BY check_in_time
                """, projectionMapper(), shiftId);
    }

    public List<OfficerShiftProjectionEntity> findByBadgeNumber(String badgeNumber) {
        return jdbcTemplate.query("""
                SELECT id, shift_id, badge_number, check_in_time, check_out_time, shift_role_type, updated_at
                FROM officer_shift_projection
                WHERE badge_number = ?
                ORDER BY check_in_time DESC
                """, projectionMapper(), badgeNumber);
    }

    public List<OfficerShiftProjectionEntity> findAll(String shiftId, String badgeNumber, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, shift_id, badge_number, check_in_time, check_out_time, shift_role_type, updated_at
                FROM officer_shift_projection
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, shiftId, badgeNumber);
        sql.append(" ORDER BY id LIMIT ? OFFSET ?");
        Object[] params = filters.withPaging(size, (long) page * size);
        return jdbcTemplate.query(sql.toString(), projectionMapper(), params);
    }

    public long count(String shiftId, String badgeNumber) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM officer_shift_projection WHERE 1=1");
        QueryFilters filters = new QueryFilters(sql, shiftId, badgeNumber);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, filters.params());
    }

    private OfficerShiftProjectionEntity mergeCheckIn(OfficerShiftProjectionEntity current, CheckInOfficerRequested event, Instant updatedAt) {
        return new OfficerShiftProjectionEntity(
                current.id(),
                event.getShiftId(),
                event.getBadgeNumber(),
                event.getCheckInTime() != null ? event.getCheckInTime() : current.checkInTime(),
                current.checkOutTime(),
                event.getShiftRoleType() != null ? event.getShiftRoleType() : current.shiftRoleType(),
                updatedAt
        );
    }

    private OfficerShiftProjectionEntity mergeCheckOut(OfficerShiftProjectionEntity current, CheckOutOfficerRequested event, Instant updatedAt) {
        return new OfficerShiftProjectionEntity(
                current.id(),
                current.shiftId(),
                current.badgeNumber(),
                current.checkInTime(),
                event.getCheckOutTime(),
                current.shiftRoleType(),
                updatedAt
        );
    }

    private OfficerShiftProjectionEntity mergeRoleUpdate(OfficerShiftProjectionEntity current, UpdateOfficerShiftRequested event, Instant updatedAt) {
        return new OfficerShiftProjectionEntity(
                current.id(),
                current.shiftId(),
                current.badgeNumber(),
                current.checkInTime(),
                current.checkOutTime(),
                event.getShiftRoleType() != null ? event.getShiftRoleType() : current.shiftRoleType(),
                updatedAt
        );
    }

    private void updateMerged(OfficerShiftProjectionEntity entity, String eventId) {
        jdbcTemplate.update("""
                        UPDATE officer_shift_projection SET
                            shift_id = ?,
                            badge_number = ?,
                            check_in_time = COALESCE(?, check_in_time),
                            check_out_time = COALESCE(?, check_out_time),
                            shift_role_type = COALESCE(?, shift_role_type),
                            updated_at = ?,
                            event_id = ?
                        WHERE id = ? AND (event_id IS NULL OR event_id != ?)
                        """,
                entity.shiftId(),
                entity.badgeNumber(),
                ts(entity.checkInTime()),
                ts(entity.checkOutTime()),
                entity.shiftRoleType(),
                ts(entity.updatedAt()),
                eventId,
                entity.id(),
                eventId);
    }

    private RowMapper<OfficerShiftProjectionEntity> projectionMapper() {
        return (rs, rowNum) -> mapProjection(rs);
    }

    private OfficerShiftProjectionEntity mapProjection(ResultSet rs) throws SQLException {
        return new OfficerShiftProjectionEntity(
                rs.getLong("id"),
                rs.getString("shift_id"),
                rs.getString("badge_number"),
                rs.getTimestamp("check_in_time") != null ? rs.getTimestamp("check_in_time").toInstant() : null,
                rs.getTimestamp("check_out_time") != null ? rs.getTimestamp("check_out_time").toInstant() : null,
                rs.getString("shift_role_type"),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private java.sql.Timestamp ts(Instant instant) {
        return instant != null ? java.sql.Timestamp.from(instant) : null;
    }

    private static class QueryFilters {
        private final Object[] params;

        QueryFilters(StringBuilder sql, String shiftId, String badgeNumber) {
            if (shiftId != null && !shiftId.isBlank()) {
                sql.append(" AND shift_id = ?");
            }
            if (badgeNumber != null && !badgeNumber.isBlank()) {
                sql.append(" AND badge_number = ?");
            }
            this.params = buildParams(shiftId, badgeNumber);
        }

        private Object[] buildParams(String shiftId, String badgeNumber) {
            if ((shiftId == null || shiftId.isBlank()) && (badgeNumber == null || badgeNumber.isBlank())) {
                return new Object[0];
            }
            if (shiftId != null && !shiftId.isBlank() && badgeNumber != null && !badgeNumber.isBlank()) {
                return new Object[]{shiftId, badgeNumber};
            }
            if (shiftId != null && !shiftId.isBlank()) {
                return new Object[]{shiftId};
            }
            return new Object[]{badgeNumber};
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
