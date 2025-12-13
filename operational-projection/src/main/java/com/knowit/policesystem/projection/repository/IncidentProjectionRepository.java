package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.incidents.ChangeIncidentStatusRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.common.events.incidents.UpdateIncidentRequested;
import com.knowit.policesystem.projection.model.IncidentProjectionEntity;
import com.knowit.policesystem.projection.model.IncidentStatusHistoryEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class IncidentProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public IncidentProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(ReportIncidentRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        INSERT INTO incident_projection (incident_id, incident_number, priority, status, reported_time, description, incident_type, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (incident_id) DO UPDATE SET
                            incident_number = EXCLUDED.incident_number,
                            priority = EXCLUDED.priority,
                            status = EXCLUDED.status,
                            reported_time = EXCLUDED.reported_time,
                            description = EXCLUDED.description,
                            incident_type = EXCLUDED.incident_type,
                            updated_at = EXCLUDED.updated_at
                        """,
                event.getIncidentId(),
                event.getIncidentNumber(),
                event.getPriority(),
                event.getStatus(),
                ts(event.getReportedTime()),
                event.getDescription(),
                event.getIncidentType(),
                ts(updatedAt));
    }

    public void applyUpdate(UpdateIncidentRequested event, Instant updatedAt) {
        IncidentProjectionEntity current = findByIncidentId(event.getIncidentId()).orElse(null);
        IncidentProjectionEntity merged = mergeUpdate(current, event, updatedAt);
        upsertMerged(merged);
    }

    public boolean changeStatus(ChangeIncidentStatusRequested event, Instant updatedAt) {
        IncidentProjectionEntity current = findByIncidentId(event.getIncidentId()).orElse(null);
        String currentStatus = current != null ? current.status() : null;
        String newStatus = event.getStatus();

        // Idempotent: only add history when status actually changes
        boolean statusChanged = newStatus != null && !newStatus.equals(currentStatus);

        IncidentProjectionEntity next = mergeStatus(current, event, updatedAt);
        upsertMerged(next);

        if (statusChanged) {
            jdbcTemplate.update("""
                    INSERT INTO incident_status_history (incident_id, status, changed_at)
                    VALUES (?, ?, ?)
                    """, event.getIncidentId(), newStatus, ts(updatedAt));
        }

        return statusChanged;
    }

    public void updateDispatchTime(String incidentId, Instant dispatchedTime, Instant updatedAt) {
        jdbcTemplate.update("""
                        UPDATE incident_projection
                        SET dispatched_time = ?, updated_at = ?
                        WHERE incident_id = ?
                        """,
                ts(dispatchedTime),
                ts(updatedAt),
                incidentId);
    }

    public void updateArrivedTime(String incidentId, Instant arrivedTime, Instant updatedAt) {
        jdbcTemplate.update("""
                        UPDATE incident_projection
                        SET arrived_time = ?, updated_at = ?
                        WHERE incident_id = ?
                        """,
                ts(arrivedTime),
                ts(updatedAt),
                incidentId);
    }

    public void updateClearedTime(String incidentId, Instant clearedTime, Instant updatedAt) {
        jdbcTemplate.update("""
                        UPDATE incident_projection
                        SET cleared_time = ?, updated_at = ?
                        WHERE incident_id = ?
                        """,
                ts(clearedTime),
                ts(updatedAt),
                incidentId);
    }

    public Optional<IncidentProjectionEntity> findByIncidentId(String incidentId) {
        List<IncidentProjectionEntity> result = jdbcTemplate.query("""
                SELECT incident_id, incident_number, priority, status, reported_time, dispatched_time, arrived_time, cleared_time, description, incident_type, updated_at
                FROM incident_projection
                WHERE incident_id = ?
                """, projectionMapper(), incidentId);
        return result.stream().findFirst();
    }

    public List<IncidentStatusHistoryEntry> findHistory(String incidentId) {
        return jdbcTemplate.query("""
                SELECT id, incident_id, status, changed_at
                FROM incident_status_history
                WHERE incident_id = ?
                ORDER BY changed_at
                """, historyMapper(), incidentId);
    }

    public List<IncidentProjectionEntity> findAll(String status, String priority, String incidentType, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT incident_id, incident_number, priority, status, reported_time, dispatched_time, arrived_time, cleared_time, description, incident_type, updated_at
                FROM incident_projection
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, status, priority, incidentType);
        sql.append(" ORDER BY incident_id LIMIT ? OFFSET ?");
        Object[] params = filters.withPaging(size, (long) page * size);
        return jdbcTemplate.query(sql.toString(), projectionMapper(), params);
    }

    public long count(String status, String priority, String incidentType) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM incident_projection WHERE 1=1");
        QueryFilters filters = new QueryFilters(sql, status, priority, incidentType);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, filters.params());
    }

    private IncidentProjectionEntity mergeUpdate(IncidentProjectionEntity current, UpdateIncidentRequested event, Instant updatedAt) {
        return new IncidentProjectionEntity(
                event.getIncidentId(),
                current != null ? current.incidentNumber() : null,
                event.getPriority() != null ? event.getPriority() : current != null ? current.priority() : null,
                current != null ? current.status() : null,
                current != null ? current.reportedTime() : null,
                current != null ? current.dispatchedTime() : null,
                current != null ? current.arrivedTime() : null,
                current != null ? current.clearedTime() : null,
                event.getDescription() != null ? event.getDescription() : current != null ? current.description() : null,
                event.getIncidentType() != null ? event.getIncidentType() : current != null ? current.incidentType() : null,
                updatedAt
        );
    }

    private IncidentProjectionEntity mergeStatus(IncidentProjectionEntity current, ChangeIncidentStatusRequested event, Instant updatedAt) {
        return new IncidentProjectionEntity(
                event.getIncidentId(),
                current != null ? current.incidentNumber() : null,
                current != null ? current.priority() : null,
                event.getStatus(),
                current != null ? current.reportedTime() : null,
                current != null ? current.dispatchedTime() : null,
                current != null ? current.arrivedTime() : null,
                current != null ? current.clearedTime() : null,
                current != null ? current.description() : null,
                current != null ? current.incidentType() : null,
                updatedAt
        );
    }

    private void upsertMerged(IncidentProjectionEntity entity) {
        jdbcTemplate.update("""
                        INSERT INTO incident_projection (incident_id, incident_number, priority, status, reported_time, dispatched_time, arrived_time, cleared_time, description, incident_type, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (incident_id) DO UPDATE SET
                            incident_number = COALESCE(EXCLUDED.incident_number, incident_projection.incident_number),
                            priority = COALESCE(EXCLUDED.priority, incident_projection.priority),
                            status = COALESCE(EXCLUDED.status, incident_projection.status),
                            reported_time = COALESCE(EXCLUDED.reported_time, incident_projection.reported_time),
                            dispatched_time = COALESCE(EXCLUDED.dispatched_time, incident_projection.dispatched_time),
                            arrived_time = COALESCE(EXCLUDED.arrived_time, incident_projection.arrived_time),
                            cleared_time = COALESCE(EXCLUDED.cleared_time, incident_projection.cleared_time),
                            description = COALESCE(EXCLUDED.description, incident_projection.description),
                            incident_type = COALESCE(EXCLUDED.incident_type, incident_projection.incident_type),
                            updated_at = EXCLUDED.updated_at
                        """,
                entity.incidentId(),
                entity.incidentNumber(),
                entity.priority(),
                entity.status(),
                ts(entity.reportedTime()),
                ts(entity.dispatchedTime()),
                ts(entity.arrivedTime()),
                ts(entity.clearedTime()),
                entity.description(),
                entity.incidentType(),
                ts(entity.updatedAt()));
    }

    private RowMapper<IncidentProjectionEntity> projectionMapper() {
        return (rs, rowNum) -> mapProjection(rs);
    }

    private IncidentProjectionEntity mapProjection(ResultSet rs) throws SQLException {
        return new IncidentProjectionEntity(
                rs.getString("incident_id"),
                rs.getString("incident_number"),
                rs.getString("priority"),
                rs.getString("status"),
                rs.getTimestamp("reported_time") != null ? rs.getTimestamp("reported_time").toInstant() : null,
                rs.getTimestamp("dispatched_time") != null ? rs.getTimestamp("dispatched_time").toInstant() : null,
                rs.getTimestamp("arrived_time") != null ? rs.getTimestamp("arrived_time").toInstant() : null,
                rs.getTimestamp("cleared_time") != null ? rs.getTimestamp("cleared_time").toInstant() : null,
                rs.getString("description"),
                rs.getString("incident_type"),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private RowMapper<IncidentStatusHistoryEntry> historyMapper() {
        return (rs, rowNum) -> mapHistory(rs);
    }

    private IncidentStatusHistoryEntry mapHistory(ResultSet rs) throws SQLException {
        return new IncidentStatusHistoryEntry(
                rs.getLong("id"),
                rs.getString("incident_id"),
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

        QueryFilters(StringBuilder sql, String status, String priority, String incidentType) {
            if (status != null && !status.isBlank()) {
                sql.append(" AND status = ?");
            }
            if (priority != null && !priority.isBlank()) {
                sql.append(" AND priority = ?");
            }
            if (incidentType != null && !incidentType.isBlank()) {
                sql.append(" AND incident_type = ?");
            }
            this.params = buildParams(status, priority, incidentType);
        }

        private Object[] buildParams(String status, String priority, String incidentType) {
            int count = 0;
            if (status != null && !status.isBlank()) count++;
            if (priority != null && !priority.isBlank()) count++;
            if (incidentType != null && !incidentType.isBlank()) count++;
            
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
            if (incidentType != null && !incidentType.isBlank()) {
                result[idx++] = incidentType;
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
