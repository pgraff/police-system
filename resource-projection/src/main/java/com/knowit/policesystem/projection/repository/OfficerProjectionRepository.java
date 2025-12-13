package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.officers.ChangeOfficerStatusRequested;
import com.knowit.policesystem.common.events.officers.RegisterOfficerRequested;
import com.knowit.policesystem.common.events.officers.UpdateOfficerRequested;
import com.knowit.policesystem.projection.model.OfficerProjectionEntity;
import com.knowit.policesystem.projection.model.OfficerStatusHistoryEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class OfficerProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public OfficerProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(RegisterOfficerRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        INSERT INTO officer_projection (badge_number, first_name, last_name, rank, email, phone_number, hire_date, status, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (badge_number) DO UPDATE SET
                            first_name = EXCLUDED.first_name,
                            last_name = EXCLUDED.last_name,
                            rank = EXCLUDED.rank,
                            email = EXCLUDED.email,
                            phone_number = EXCLUDED.phone_number,
                            hire_date = EXCLUDED.hire_date,
                            status = EXCLUDED.status,
                            updated_at = EXCLUDED.updated_at
                        """,
                event.getBadgeNumber(),
                event.getFirstName(),
                event.getLastName(),
                event.getRank(),
                event.getEmail(),
                event.getPhoneNumber(),
                event.getHireDate(),
                event.getStatus(),
                ts(updatedAt));
    }

    public void applyUpdate(UpdateOfficerRequested event, Instant updatedAt) {
        OfficerProjectionEntity current = findByBadgeNumber(event.getBadgeNumber()).orElse(null);
        OfficerProjectionEntity merged = mergeUpdate(current, event, updatedAt);
        upsertMerged(merged);
    }

    public boolean changeStatus(ChangeOfficerStatusRequested event, Instant updatedAt) {
        OfficerProjectionEntity current = findByBadgeNumber(event.getBadgeNumber()).orElse(null);
        String currentStatus = current != null ? current.status() : null;
        String newStatus = event.getStatus();

        // Idempotent: only add history when status actually changes
        boolean statusChanged = newStatus != null && !newStatus.equals(currentStatus);

        OfficerProjectionEntity next = mergeStatus(current, event, updatedAt);
        upsertMerged(next);

        if (statusChanged) {
            jdbcTemplate.update("""
                    INSERT INTO officer_status_history (badge_number, status, changed_at)
                    VALUES (?, ?, ?)
                    """, event.getBadgeNumber(), newStatus, ts(updatedAt));
        }

        return statusChanged;
    }

    public Optional<OfficerProjectionEntity> findByBadgeNumber(String badgeNumber) {
        List<OfficerProjectionEntity> result = jdbcTemplate.query("""
                SELECT badge_number, first_name, last_name, rank, email, phone_number, hire_date, status, updated_at
                FROM officer_projection
                WHERE badge_number = ?
                """, projectionMapper(), badgeNumber);
        return result.stream().findFirst();
    }

    public List<OfficerStatusHistoryEntry> findHistory(String badgeNumber) {
        return jdbcTemplate.query("""
                SELECT status, changed_at
                FROM officer_status_history
                WHERE badge_number = ?
                ORDER BY changed_at
                """, historyMapper(), badgeNumber);
    }

    public List<OfficerProjectionEntity> findAll(String status, String rank, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT badge_number, first_name, last_name, rank, email, phone_number, hire_date, status, updated_at
                FROM officer_projection
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, status, rank);
        sql.append(" ORDER BY badge_number LIMIT ? OFFSET ?");
        Object[] params = filters.withPaging(size, (long) page * size);
        return jdbcTemplate.query(sql.toString(), projectionMapper(), params);
    }

    public long count(String status, String rank) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM officer_projection WHERE 1=1");
        QueryFilters filters = new QueryFilters(sql, status, rank);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, filters.params());
    }

    private OfficerProjectionEntity mergeUpdate(OfficerProjectionEntity current, UpdateOfficerRequested event, Instant updatedAt) {
        return new OfficerProjectionEntity(
                event.getBadgeNumber(),
                event.getFirstName() != null ? event.getFirstName() : current != null ? current.firstName() : null,
                event.getLastName() != null ? event.getLastName() : current != null ? current.lastName() : null,
                event.getRank() != null ? event.getRank() : current != null ? current.rank() : null,
                event.getEmail() != null ? event.getEmail() : current != null ? current.email() : null,
                event.getPhoneNumber() != null ? event.getPhoneNumber() : current != null ? current.phoneNumber() : null,
                event.getHireDate() != null ? event.getHireDate() : current != null ? current.hireDate() : null,
                current != null ? current.status() : null,
                updatedAt
        );
    }

    private OfficerProjectionEntity mergeStatus(OfficerProjectionEntity current, ChangeOfficerStatusRequested event, Instant updatedAt) {
        return new OfficerProjectionEntity(
                event.getBadgeNumber(),
                current != null ? current.firstName() : null,
                current != null ? current.lastName() : null,
                current != null ? current.rank() : null,
                current != null ? current.email() : null,
                current != null ? current.phoneNumber() : null,
                current != null ? current.hireDate() : null,
                event.getStatus(),
                updatedAt
        );
    }

    private void upsertMerged(OfficerProjectionEntity entity) {
        jdbcTemplate.update("""
                        INSERT INTO officer_projection (badge_number, first_name, last_name, rank, email, phone_number, hire_date, status, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (badge_number) DO UPDATE SET
                            first_name = COALESCE(EXCLUDED.first_name, officer_projection.first_name),
                            last_name = COALESCE(EXCLUDED.last_name, officer_projection.last_name),
                            rank = COALESCE(EXCLUDED.rank, officer_projection.rank),
                            email = COALESCE(EXCLUDED.email, officer_projection.email),
                            phone_number = COALESCE(EXCLUDED.phone_number, officer_projection.phone_number),
                            hire_date = COALESCE(EXCLUDED.hire_date, officer_projection.hire_date),
                            status = COALESCE(EXCLUDED.status, officer_projection.status),
                            updated_at = EXCLUDED.updated_at
                        """,
                entity.badgeNumber(),
                entity.firstName(),
                entity.lastName(),
                entity.rank(),
                entity.email(),
                entity.phoneNumber(),
                entity.hireDate(),
                entity.status(),
                ts(entity.updatedAt()));
    }

    private RowMapper<OfficerProjectionEntity> projectionMapper() {
        return (rs, rowNum) -> mapProjection(rs);
    }

    private OfficerProjectionEntity mapProjection(ResultSet rs) throws SQLException {
        return new OfficerProjectionEntity(
                rs.getString("badge_number"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("rank"),
                rs.getString("email"),
                rs.getString("phone_number"),
                rs.getString("hire_date"),
                rs.getString("status"),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private RowMapper<OfficerStatusHistoryEntry> historyMapper() {
        return (rs, rowNum) -> mapHistory(rs);
    }

    private OfficerStatusHistoryEntry mapHistory(ResultSet rs) throws SQLException {
        return new OfficerStatusHistoryEntry(
                rs.getString("status"),
                rs.getTimestamp("changed_at").toInstant()
        );
    }

    private java.sql.Timestamp ts(Instant instant) {
        return instant != null ? java.sql.Timestamp.from(instant) : null;
    }

    private static class QueryFilters {
        private final Object[] params;

        QueryFilters(StringBuilder sql, String status, String rank) {
            if (status != null && !status.isBlank()) {
                sql.append(" AND status = ?");
            }
            if (rank != null && !rank.isBlank()) {
                sql.append(" AND rank = ?");
            }
            this.params = buildParams(status, rank);
        }

        private Object[] buildParams(String status, String rank) {
            if ((status == null || status.isBlank()) && (rank == null || rank.isBlank())) {
                return new Object[0];
            }
            if (status != null && !status.isBlank() && rank != null && !rank.isBlank()) {
                return new Object[]{status, rank};
            }
            if (status != null && !status.isBlank()) {
                return new Object[]{status};
            }
            return new Object[]{rank};
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
