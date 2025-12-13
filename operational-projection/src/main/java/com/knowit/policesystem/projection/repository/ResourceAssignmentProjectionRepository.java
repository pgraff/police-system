package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.resourceassignment.AssignResourceRequested;
import com.knowit.policesystem.common.events.resourceassignment.ChangeResourceAssignmentStatusRequested;
import com.knowit.policesystem.common.events.resourceassignment.UnassignResourceRequested;
import com.knowit.policesystem.projection.model.ResourceAssignmentProjectionEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class ResourceAssignmentProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public ResourceAssignmentProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(AssignResourceRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        INSERT INTO resource_assignment_projection (assignment_id, resource_id, resource_type, role_type, status, start_time, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (assignment_id, resource_id, resource_type) DO UPDATE SET
                            role_type = EXCLUDED.role_type,
                            status = EXCLUDED.status,
                            start_time = EXCLUDED.start_time,
                            updated_at = EXCLUDED.updated_at
                        """,
                event.getAssignmentId(),
                event.getResourceId(),
                event.getResourceType(),
                event.getRoleType(),
                event.getStatus(),
                ts(event.getStartTime()),
                ts(updatedAt));
    }

    public void changeStatus(ChangeResourceAssignmentStatusRequested event, Instant updatedAt) {
        // Find existing record to get resourceType (not provided in event)
        // Query by assignmentId and resourceId - should be unique per assignment
        List<ResourceAssignmentProjectionEntity> existing = jdbcTemplate.query("""
                SELECT id, assignment_id, resource_id, resource_type, role_type, status, start_time, end_time, updated_at
                FROM resource_assignment_projection
                WHERE assignment_id = ? AND resource_id = ?
                """, projectionMapper(), event.getAssignmentId(), event.getResourceId());
        
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Resource assignment not found for assignmentId=" + event.getAssignmentId() + ", resourceId=" + event.getResourceId());
        }
        
        String resourceType = existing.get(0).resourceType();
        
        jdbcTemplate.update("""
                        UPDATE resource_assignment_projection
                        SET status = ?, updated_at = ?
                        WHERE assignment_id = ? AND resource_id = ? AND resource_type = ?
                        """,
                event.getStatus(),
                ts(updatedAt),
                event.getAssignmentId(),
                event.getResourceId(),
                resourceType);
    }

    public void unassign(UnassignResourceRequested event, Instant updatedAt) {
        // Find existing record to get resourceType (not provided in event)
        // Query by assignmentId and resourceId - should be unique per assignment
        List<ResourceAssignmentProjectionEntity> existing = jdbcTemplate.query("""
                SELECT id, assignment_id, resource_id, resource_type, role_type, status, start_time, end_time, updated_at
                FROM resource_assignment_projection
                WHERE assignment_id = ? AND resource_id = ?
                """, projectionMapper(), event.getAssignmentId(), event.getResourceId());
        
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Resource assignment not found for assignmentId=" + event.getAssignmentId() + ", resourceId=" + event.getResourceId());
        }
        
        String resourceType = existing.get(0).resourceType();
        
        jdbcTemplate.update("""
                        UPDATE resource_assignment_projection
                        SET end_time = ?, updated_at = ?
                        WHERE assignment_id = ? AND resource_id = ? AND resource_type = ?
                        """,
                ts(event.getEndTime()),
                ts(updatedAt),
                event.getAssignmentId(),
                event.getResourceId(),
                resourceType);
    }

    public Optional<ResourceAssignmentProjectionEntity> findById(Long id) {
        List<ResourceAssignmentProjectionEntity> result = jdbcTemplate.query("""
                SELECT id, assignment_id, resource_id, resource_type, role_type, status, start_time, end_time, updated_at
                FROM resource_assignment_projection
                WHERE id = ?
                """, projectionMapper(), id);
        return result.stream().findFirst();
    }

    public Optional<ResourceAssignmentProjectionEntity> findByCompositeKey(String assignmentId, String resourceId, String resourceType) {
        List<ResourceAssignmentProjectionEntity> result = jdbcTemplate.query("""
                SELECT id, assignment_id, resource_id, resource_type, role_type, status, start_time, end_time, updated_at
                FROM resource_assignment_projection
                WHERE assignment_id = ? AND resource_id = ? AND resource_type = ?
                """, projectionMapper(), assignmentId, resourceId, resourceType);
        return result.stream().findFirst();
    }

    public List<ResourceAssignmentProjectionEntity> findByAssignmentId(String assignmentId) {
        return jdbcTemplate.query("""
                SELECT id, assignment_id, resource_id, resource_type, role_type, status, start_time, end_time, updated_at
                FROM resource_assignment_projection
                WHERE assignment_id = ?
                ORDER BY start_time DESC NULLS LAST
                """, projectionMapper(), assignmentId);
    }

    public List<ResourceAssignmentProjectionEntity> findByResourceId(String resourceId, String resourceType) {
        return jdbcTemplate.query("""
                SELECT id, assignment_id, resource_id, resource_type, role_type, status, start_time, end_time, updated_at
                FROM resource_assignment_projection
                WHERE resource_id = ? AND resource_type = ?
                ORDER BY start_time DESC NULLS LAST
                """, projectionMapper(), resourceId, resourceType);
    }

    public List<ResourceAssignmentProjectionEntity> findAll(String assignmentId, String resourceId, String resourceType,
                                                           String roleType, String status, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, assignment_id, resource_id, resource_type, role_type, status, start_time, end_time, updated_at
                FROM resource_assignment_projection
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, assignmentId, resourceId, resourceType, roleType, status);
        sql.append(" ORDER BY start_time DESC NULLS LAST, id LIMIT ? OFFSET ?");
        Object[] params = filters.withPaging(size, (long) page * size);
        return jdbcTemplate.query(sql.toString(), projectionMapper(), params);
    }

    public long count(String assignmentId, String resourceId, String resourceType, String roleType, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM resource_assignment_projection WHERE 1=1");
        QueryFilters filters = new QueryFilters(sql, assignmentId, resourceId, resourceType, roleType, status);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, filters.params());
    }

    private RowMapper<ResourceAssignmentProjectionEntity> projectionMapper() {
        return (rs, rowNum) -> mapProjection(rs);
    }

    private ResourceAssignmentProjectionEntity mapProjection(ResultSet rs) throws SQLException {
        return new ResourceAssignmentProjectionEntity(
                rs.getLong("id"),
                rs.getString("assignment_id"),
                rs.getString("resource_id"),
                rs.getString("resource_type"),
                rs.getString("role_type"),
                rs.getString("status"),
                rs.getTimestamp("start_time") != null ? rs.getTimestamp("start_time").toInstant() : null,
                rs.getTimestamp("end_time") != null ? rs.getTimestamp("end_time").toInstant() : null,
                rs.getTimestamp("updated_at").toInstant()
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

        QueryFilters(StringBuilder sql, String assignmentId, String resourceId, String resourceType, String roleType, String status) {
            if (assignmentId != null && !assignmentId.isBlank()) {
                sql.append(" AND assignment_id = ?");
            }
            if (resourceId != null && !resourceId.isBlank()) {
                sql.append(" AND resource_id = ?");
            }
            if (resourceType != null && !resourceType.isBlank()) {
                sql.append(" AND resource_type = ?");
            }
            if (roleType != null && !roleType.isBlank()) {
                sql.append(" AND role_type = ?");
            }
            if (status != null && !status.isBlank()) {
                sql.append(" AND status = ?");
            }
            this.params = buildParams(assignmentId, resourceId, resourceType, roleType, status);
        }

        private Object[] buildParams(String assignmentId, String resourceId, String resourceType, String roleType, String status) {
            int count = 0;
            if (assignmentId != null && !assignmentId.isBlank()) count++;
            if (resourceId != null && !resourceId.isBlank()) count++;
            if (resourceType != null && !resourceType.isBlank()) count++;
            if (roleType != null && !roleType.isBlank()) count++;
            if (status != null && !status.isBlank()) count++;
            
            if (count == 0) {
                return new Object[0];
            }
            
            Object[] result = new Object[count];
            int idx = 0;
            if (assignmentId != null && !assignmentId.isBlank()) {
                result[idx++] = assignmentId;
            }
            if (resourceId != null && !resourceId.isBlank()) {
                result[idx++] = resourceId;
            }
            if (resourceType != null && !resourceType.isBlank()) {
                result[idx++] = resourceType;
            }
            if (roleType != null && !roleType.isBlank()) {
                result[idx++] = roleType;
            }
            if (status != null && !status.isBlank()) {
                result[idx++] = status;
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
