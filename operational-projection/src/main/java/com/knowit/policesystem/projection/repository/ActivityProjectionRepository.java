package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.activities.ChangeActivityStatusRequested;
import com.knowit.policesystem.common.events.activities.LinkActivityToIncidentRequested;
import com.knowit.policesystem.common.events.activities.StartActivityRequested;
import com.knowit.policesystem.common.events.activities.UpdateActivityRequested;
import com.knowit.policesystem.projection.model.ActivityProjectionEntity;
import com.knowit.policesystem.projection.model.ActivityStatusHistoryEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class ActivityProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public ActivityProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(StartActivityRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        INSERT INTO activity_projection (activity_id, activity_time, activity_type, description, status, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT (activity_id) DO UPDATE SET
                            activity_time = EXCLUDED.activity_time,
                            activity_type = EXCLUDED.activity_type,
                            description = EXCLUDED.description,
                            status = EXCLUDED.status,
                            updated_at = EXCLUDED.updated_at
                        """,
                event.getActivityId(),
                ts(event.getActivityTime()),
                event.getActivityType(),
                event.getDescription(),
                event.getStatus(),
                ts(updatedAt));
    }

    public void applyUpdate(UpdateActivityRequested event, Instant updatedAt) {
        ActivityProjectionEntity current = findByActivityId(event.getActivityId()).orElse(null);
        ActivityProjectionEntity merged = mergeUpdate(current, event, updatedAt);
        upsertMerged(merged);
    }

    public boolean changeStatus(ChangeActivityStatusRequested event, Instant updatedAt) {
        ActivityProjectionEntity current = findByActivityId(event.getActivityId()).orElse(null);
        String currentStatus = current != null ? current.status() : null;
        String newStatus = event.getStatus();

        // Idempotent: only add history when status actually changes
        boolean statusChanged = newStatus != null && !newStatus.equals(currentStatus);

        ActivityProjectionEntity next = mergeStatus(current, event, updatedAt);
        upsertMerged(next);

        if (statusChanged) {
            jdbcTemplate.update("""
                    INSERT INTO activity_status_history (activity_id, status, changed_at)
                    VALUES (?, ?, ?)
                    """, event.getActivityId(), newStatus, ts(updatedAt));
        }

        return statusChanged;
    }

    public void updateCompletedTime(String activityId, Instant completedTime, Instant updatedAt) {
        jdbcTemplate.update("""
                        UPDATE activity_projection
                        SET completed_time = ?, updated_at = ?
                        WHERE activity_id = ?
                        """,
                ts(completedTime),
                ts(updatedAt),
                activityId);
    }

    public void linkToIncident(LinkActivityToIncidentRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        UPDATE activity_projection
                        SET incident_id = ?, updated_at = ?
                        WHERE activity_id = ?
                        """,
                event.getIncidentId(),
                ts(updatedAt),
                event.getActivityId());
    }

    public Optional<ActivityProjectionEntity> findByActivityId(String activityId) {
        List<ActivityProjectionEntity> result = jdbcTemplate.query("""
                SELECT activity_id, activity_time, activity_type, description, status, completed_time, incident_id, updated_at
                FROM activity_projection
                WHERE activity_id = ?
                """, projectionMapper(), activityId);
        return result.stream().findFirst();
    }

    public List<ActivityProjectionEntity> findByIncidentId(String incidentId) {
        return jdbcTemplate.query("""
                SELECT activity_id, activity_time, activity_type, description, status, completed_time, incident_id, updated_at
                FROM activity_projection
                WHERE incident_id = ?
                ORDER BY activity_time DESC NULLS LAST
                """, projectionMapper(), incidentId);
    }

    public List<ActivityStatusHistoryEntry> findHistory(String activityId) {
        return jdbcTemplate.query("""
                SELECT id, activity_id, status, changed_at
                FROM activity_status_history
                WHERE activity_id = ?
                ORDER BY changed_at
                """, historyMapper(), activityId);
    }

    public List<ActivityProjectionEntity> findAll(String status, String activityType, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT activity_id, activity_time, activity_type, description, status, completed_time, incident_id, updated_at
                FROM activity_projection
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, status, activityType);
        sql.append(" ORDER BY activity_id LIMIT ? OFFSET ?");
        Object[] params = filters.withPaging(size, (long) page * size);
        return jdbcTemplate.query(sql.toString(), projectionMapper(), params);
    }

    public long count(String status, String activityType) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM activity_projection WHERE 1=1");
        QueryFilters filters = new QueryFilters(sql, status, activityType);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, filters.params());
    }

    private ActivityProjectionEntity mergeUpdate(ActivityProjectionEntity current, UpdateActivityRequested event, Instant updatedAt) {
        return new ActivityProjectionEntity(
                event.getActivityId(),
                current != null ? current.activityTime() : null,
                current != null ? current.activityType() : null,
                event.getDescription() != null ? event.getDescription() : current != null ? current.description() : null,
                current != null ? current.status() : null,
                current != null ? current.completedTime() : null,
                current != null ? current.incidentId() : null,
                updatedAt
        );
    }

    private ActivityProjectionEntity mergeStatus(ActivityProjectionEntity current, ChangeActivityStatusRequested event, Instant updatedAt) {
        return new ActivityProjectionEntity(
                event.getActivityId(),
                current != null ? current.activityTime() : null,
                current != null ? current.activityType() : null,
                current != null ? current.description() : null,
                event.getStatus(),
                current != null ? current.completedTime() : null,
                current != null ? current.incidentId() : null,
                updatedAt
        );
    }

    private void upsertMerged(ActivityProjectionEntity entity) {
        jdbcTemplate.update("""
                        INSERT INTO activity_projection (activity_id, activity_time, activity_type, description, status, completed_time, incident_id, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (activity_id) DO UPDATE SET
                            activity_time = COALESCE(EXCLUDED.activity_time, activity_projection.activity_time),
                            activity_type = COALESCE(EXCLUDED.activity_type, activity_projection.activity_type),
                            description = COALESCE(EXCLUDED.description, activity_projection.description),
                            status = COALESCE(EXCLUDED.status, activity_projection.status),
                            completed_time = COALESCE(EXCLUDED.completed_time, activity_projection.completed_time),
                            incident_id = COALESCE(EXCLUDED.incident_id, activity_projection.incident_id),
                            updated_at = EXCLUDED.updated_at
                        """,
                entity.activityId(),
                ts(entity.activityTime()),
                entity.activityType(),
                entity.description(),
                entity.status(),
                ts(entity.completedTime()),
                entity.incidentId(),
                ts(entity.updatedAt()));
    }

    private RowMapper<ActivityProjectionEntity> projectionMapper() {
        return (rs, rowNum) -> mapProjection(rs);
    }

    private ActivityProjectionEntity mapProjection(ResultSet rs) throws SQLException {
        return new ActivityProjectionEntity(
                rs.getString("activity_id"),
                rs.getTimestamp("activity_time") != null ? rs.getTimestamp("activity_time").toInstant() : null,
                rs.getString("activity_type"),
                rs.getString("description"),
                rs.getString("status"),
                rs.getTimestamp("completed_time") != null ? rs.getTimestamp("completed_time").toInstant() : null,
                rs.getString("incident_id"),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private RowMapper<ActivityStatusHistoryEntry> historyMapper() {
        return (rs, rowNum) -> mapHistory(rs);
    }

    private ActivityStatusHistoryEntry mapHistory(ResultSet rs) throws SQLException {
        return new ActivityStatusHistoryEntry(
                rs.getLong("id"),
                rs.getString("activity_id"),
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

        QueryFilters(StringBuilder sql, String status, String activityType) {
            if (status != null && !status.isBlank()) {
                sql.append(" AND status = ?");
            }
            if (activityType != null && !activityType.isBlank()) {
                sql.append(" AND activity_type = ?");
            }
            this.params = buildParams(status, activityType);
        }

        private Object[] buildParams(String status, String activityType) {
            int count = 0;
            if (status != null && !status.isBlank()) count++;
            if (activityType != null && !activityType.isBlank()) count++;
            
            if (count == 0) {
                return new Object[0];
            }
            
            Object[] result = new Object[count];
            int idx = 0;
            if (status != null && !status.isBlank()) {
                result[idx++] = status;
            }
            if (activityType != null && !activityType.isBlank()) {
                result[idx++] = activityType;
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
