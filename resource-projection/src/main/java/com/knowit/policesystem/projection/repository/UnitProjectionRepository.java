package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.units.ChangeUnitStatusRequested;
import com.knowit.policesystem.common.events.units.CreateUnitRequested;
import com.knowit.policesystem.common.events.units.UpdateUnitRequested;
import com.knowit.policesystem.projection.model.UnitProjectionEntity;
import com.knowit.policesystem.projection.model.UnitStatusHistoryEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class UnitProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public UnitProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(CreateUnitRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        INSERT INTO unit_projection (unit_id, unit_type, status, updated_at)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT (unit_id) DO UPDATE SET
                            unit_type = EXCLUDED.unit_type,
                            status = EXCLUDED.status,
                            updated_at = EXCLUDED.updated_at
                        """,
                event.getUnitId(),
                event.getUnitType(),
                event.getStatus(),
                ts(updatedAt));
    }

    public void applyUpdate(UpdateUnitRequested event, Instant updatedAt) {
        UnitProjectionEntity current = findByUnitId(event.getUnitId()).orElse(null);
        UnitProjectionEntity merged = mergeUpdate(current, event, updatedAt);
        upsertMerged(merged);
    }

    public boolean changeStatus(ChangeUnitStatusRequested event, Instant updatedAt) {
        UnitProjectionEntity current = findByUnitId(event.getUnitId()).orElse(null);
        String currentStatus = current != null ? current.status() : null;
        String newStatus = event.getStatus();

        // Idempotent: only add history when status actually changes
        boolean statusChanged = newStatus != null && !newStatus.equals(currentStatus);

        UnitProjectionEntity next = mergeStatus(current, event, updatedAt);
        upsertMerged(next);

        if (statusChanged) {
            jdbcTemplate.update("""
                    INSERT INTO unit_status_history (unit_id, status, changed_at)
                    VALUES (?, ?, ?)
                    """, event.getUnitId(), newStatus, ts(updatedAt));
        }

        return statusChanged;
    }

    public Optional<UnitProjectionEntity> findByUnitId(String unitId) {
        List<UnitProjectionEntity> result = jdbcTemplate.query("""
                SELECT unit_id, unit_type, status, updated_at
                FROM unit_projection
                WHERE unit_id = ?
                """, projectionMapper(), unitId);
        return result.stream().findFirst();
    }

    public List<UnitStatusHistoryEntry> findHistory(String unitId) {
        return jdbcTemplate.query("""
                SELECT status, changed_at
                FROM unit_status_history
                WHERE unit_id = ?
                ORDER BY changed_at
                """, historyMapper(), unitId);
    }

    public List<UnitProjectionEntity> findAll(String status, String unitType, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT unit_id, unit_type, status, updated_at
                FROM unit_projection
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, status, unitType);
        sql.append(" ORDER BY unit_id LIMIT ? OFFSET ?");
        Object[] params = filters.withPaging(size, (long) page * size);
        return jdbcTemplate.query(sql.toString(), projectionMapper(), params);
    }

    public long count(String status, String unitType) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM unit_projection WHERE 1=1");
        QueryFilters filters = new QueryFilters(sql, status, unitType);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, filters.params());
    }

    private UnitProjectionEntity mergeUpdate(UnitProjectionEntity current, UpdateUnitRequested event, Instant updatedAt) {
        return new UnitProjectionEntity(
                event.getUnitId(),
                event.getUnitType() != null ? event.getUnitType() : current != null ? current.unitType() : null,
                current != null ? current.status() : null,
                updatedAt
        );
    }

    private UnitProjectionEntity mergeStatus(UnitProjectionEntity current, ChangeUnitStatusRequested event, Instant updatedAt) {
        return new UnitProjectionEntity(
                event.getUnitId(),
                current != null ? current.unitType() : null,
                event.getStatus(),
                updatedAt
        );
    }

    private void upsertMerged(UnitProjectionEntity entity) {
        jdbcTemplate.update("""
                        INSERT INTO unit_projection (unit_id, unit_type, status, updated_at)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT (unit_id) DO UPDATE SET
                            unit_type = COALESCE(EXCLUDED.unit_type, unit_projection.unit_type),
                            status = COALESCE(EXCLUDED.status, unit_projection.status),
                            updated_at = EXCLUDED.updated_at
                        """,
                entity.unitId(),
                entity.unitType(),
                entity.status(),
                ts(entity.updatedAt()));
    }

    private RowMapper<UnitProjectionEntity> projectionMapper() {
        return (rs, rowNum) -> mapProjection(rs);
    }

    private UnitProjectionEntity mapProjection(ResultSet rs) throws SQLException {
        return new UnitProjectionEntity(
                rs.getString("unit_id"),
                rs.getString("unit_type"),
                rs.getString("status"),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private RowMapper<UnitStatusHistoryEntry> historyMapper() {
        return (rs, rowNum) -> mapHistory(rs);
    }

    private UnitStatusHistoryEntry mapHistory(ResultSet rs) throws SQLException {
        return new UnitStatusHistoryEntry(
                rs.getString("status"),
                rs.getTimestamp("changed_at").toInstant()
        );
    }

    private java.sql.Timestamp ts(Instant instant) {
        return instant != null ? java.sql.Timestamp.from(instant) : null;
    }

    private static class QueryFilters {
        private final Object[] params;

        QueryFilters(StringBuilder sql, String status, String unitType) {
            if (status != null && !status.isBlank()) {
                sql.append(" AND status = ?");
            }
            if (unitType != null && !unitType.isBlank()) {
                sql.append(" AND unit_type = ?");
            }
            this.params = buildParams(status, unitType);
        }

        private Object[] buildParams(String status, String unitType) {
            if ((status == null || status.isBlank()) && (unitType == null || unitType.isBlank())) {
                return new Object[0];
            }
            if (status != null && !status.isBlank() && unitType != null && !unitType.isBlank()) {
                return new Object[]{status, unitType};
            }
            if (status != null && !status.isBlank()) {
                return new Object[]{status};
            }
            return new Object[]{unitType};
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
