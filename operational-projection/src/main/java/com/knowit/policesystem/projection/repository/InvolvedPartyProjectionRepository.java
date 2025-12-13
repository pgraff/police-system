package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.involvedparty.EndPartyInvolvementRequested;
import com.knowit.policesystem.common.events.involvedparty.InvolvePartyRequested;
import com.knowit.policesystem.common.events.involvedparty.UpdatePartyInvolvementRequested;
import com.knowit.policesystem.projection.model.InvolvedPartyProjectionEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class InvolvedPartyProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public InvolvedPartyProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(InvolvePartyRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        INSERT INTO involved_party_projection (involvement_id, person_id, party_role_type, description, involvement_start_time, incident_id, call_id, activity_id, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (involvement_id) DO UPDATE SET
                            person_id = EXCLUDED.person_id,
                            party_role_type = EXCLUDED.party_role_type,
                            description = EXCLUDED.description,
                            involvement_start_time = EXCLUDED.involvement_start_time,
                            incident_id = EXCLUDED.incident_id,
                            call_id = EXCLUDED.call_id,
                            activity_id = EXCLUDED.activity_id,
                            updated_at = EXCLUDED.updated_at
                        """,
                event.getInvolvementId(),
                event.getPersonId(),
                event.getPartyRoleType(),
                event.getDescription(),
                ts(event.getInvolvementStartTime()),
                event.getIncidentId(),
                event.getCallId(),
                event.getActivityId(),
                ts(updatedAt));
    }

    public void applyUpdate(UpdatePartyInvolvementRequested event, Instant updatedAt) {
        InvolvedPartyProjectionEntity current = findByInvolvementId(event.getInvolvementId()).orElse(null);
        InvolvedPartyProjectionEntity merged = mergeUpdate(current, event, updatedAt);
        upsertMerged(merged);
    }

    public void endInvolvement(EndPartyInvolvementRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        UPDATE involved_party_projection
                        SET involvement_end_time = ?, updated_at = ?
                        WHERE involvement_id = ?
                        """,
                ts(event.getInvolvementEndTime()),
                ts(updatedAt),
                event.getInvolvementId());
    }

    public Optional<InvolvedPartyProjectionEntity> findByInvolvementId(String involvementId) {
        List<InvolvedPartyProjectionEntity> result = jdbcTemplate.query("""
                SELECT involvement_id, person_id, party_role_type, description, involvement_start_time, involvement_end_time, incident_id, call_id, activity_id, updated_at
                FROM involved_party_projection
                WHERE involvement_id = ?
                """, projectionMapper(), involvementId);
        return result.stream().findFirst();
    }

    public List<InvolvedPartyProjectionEntity> findByPersonId(String personId) {
        return jdbcTemplate.query("""
                SELECT involvement_id, person_id, party_role_type, description, involvement_start_time, involvement_end_time, incident_id, call_id, activity_id, updated_at
                FROM involved_party_projection
                WHERE person_id = ?
                ORDER BY involvement_start_time DESC NULLS LAST
                """, projectionMapper(), personId);
    }

    public List<InvolvedPartyProjectionEntity> findByIncidentId(String incidentId) {
        return jdbcTemplate.query("""
                SELECT involvement_id, person_id, party_role_type, description, involvement_start_time, involvement_end_time, incident_id, call_id, activity_id, updated_at
                FROM involved_party_projection
                WHERE incident_id = ?
                ORDER BY involvement_start_time DESC NULLS LAST
                """, projectionMapper(), incidentId);
    }

    public List<InvolvedPartyProjectionEntity> findByCallId(String callId) {
        return jdbcTemplate.query("""
                SELECT involvement_id, person_id, party_role_type, description, involvement_start_time, involvement_end_time, incident_id, call_id, activity_id, updated_at
                FROM involved_party_projection
                WHERE call_id = ?
                ORDER BY involvement_start_time DESC NULLS LAST
                """, projectionMapper(), callId);
    }

    public List<InvolvedPartyProjectionEntity> findByActivityId(String activityId) {
        return jdbcTemplate.query("""
                SELECT involvement_id, person_id, party_role_type, description, involvement_start_time, involvement_end_time, incident_id, call_id, activity_id, updated_at
                FROM involved_party_projection
                WHERE activity_id = ?
                ORDER BY involvement_start_time DESC NULLS LAST
                """, projectionMapper(), activityId);
    }

    public List<InvolvedPartyProjectionEntity> findAll(String personId, String partyRoleType, String incidentId,
                                                       String callId, String activityId, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT involvement_id, person_id, party_role_type, description, involvement_start_time, involvement_end_time, incident_id, call_id, activity_id, updated_at
                FROM involved_party_projection
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, personId, partyRoleType, incidentId, callId, activityId);
        sql.append(" ORDER BY involvement_start_time DESC NULLS LAST, involvement_id LIMIT ? OFFSET ?");
        Object[] params = filters.withPaging(size, (long) page * size);
        return jdbcTemplate.query(sql.toString(), projectionMapper(), params);
    }

    public long count(String personId, String partyRoleType, String incidentId, String callId, String activityId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM involved_party_projection WHERE 1=1");
        QueryFilters filters = new QueryFilters(sql, personId, partyRoleType, incidentId, callId, activityId);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, filters.params());
    }

    private InvolvedPartyProjectionEntity mergeUpdate(InvolvedPartyProjectionEntity current, UpdatePartyInvolvementRequested event, Instant updatedAt) {
        return new InvolvedPartyProjectionEntity(
                event.getInvolvementId(),
                current != null ? current.personId() : null,
                event.getPartyRoleType() != null ? event.getPartyRoleType() : current != null ? current.partyRoleType() : null,
                event.getDescription() != null ? event.getDescription() : current != null ? current.description() : null,
                current != null ? current.involvementStartTime() : null,
                current != null ? current.involvementEndTime() : null,
                current != null ? current.incidentId() : null,
                current != null ? current.callId() : null,
                current != null ? current.activityId() : null,
                updatedAt
        );
    }

    private void upsertMerged(InvolvedPartyProjectionEntity entity) {
        jdbcTemplate.update("""
                        INSERT INTO involved_party_projection (involvement_id, person_id, party_role_type, description, involvement_start_time, involvement_end_time, incident_id, call_id, activity_id, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (involvement_id) DO UPDATE SET
                            person_id = COALESCE(EXCLUDED.person_id, involved_party_projection.person_id),
                            party_role_type = COALESCE(EXCLUDED.party_role_type, involved_party_projection.party_role_type),
                            description = COALESCE(EXCLUDED.description, involved_party_projection.description),
                            involvement_start_time = COALESCE(EXCLUDED.involvement_start_time, involved_party_projection.involvement_start_time),
                            involvement_end_time = COALESCE(EXCLUDED.involvement_end_time, involved_party_projection.involvement_end_time),
                            incident_id = COALESCE(EXCLUDED.incident_id, involved_party_projection.incident_id),
                            call_id = COALESCE(EXCLUDED.call_id, involved_party_projection.call_id),
                            activity_id = COALESCE(EXCLUDED.activity_id, involved_party_projection.activity_id),
                            updated_at = EXCLUDED.updated_at
                        """,
                entity.involvementId(),
                entity.personId(),
                entity.partyRoleType(),
                entity.description(),
                ts(entity.involvementStartTime()),
                ts(entity.involvementEndTime()),
                entity.incidentId(),
                entity.callId(),
                entity.activityId(),
                ts(entity.updatedAt()));
    }

    private RowMapper<InvolvedPartyProjectionEntity> projectionMapper() {
        return (rs, rowNum) -> mapProjection(rs);
    }

    private InvolvedPartyProjectionEntity mapProjection(ResultSet rs) throws SQLException {
        return new InvolvedPartyProjectionEntity(
                rs.getString("involvement_id"),
                rs.getString("person_id"),
                rs.getString("party_role_type"),
                rs.getString("description"),
                rs.getTimestamp("involvement_start_time") != null ? rs.getTimestamp("involvement_start_time").toInstant() : null,
                rs.getTimestamp("involvement_end_time") != null ? rs.getTimestamp("involvement_end_time").toInstant() : null,
                rs.getString("incident_id"),
                rs.getString("call_id"),
                rs.getString("activity_id"),
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

        QueryFilters(StringBuilder sql, String personId, String partyRoleType, String incidentId, String callId, String activityId) {
            if (personId != null && !personId.isBlank()) {
                sql.append(" AND person_id = ?");
            }
            if (partyRoleType != null && !partyRoleType.isBlank()) {
                sql.append(" AND party_role_type = ?");
            }
            if (incidentId != null && !incidentId.isBlank()) {
                sql.append(" AND incident_id = ?");
            }
            if (callId != null && !callId.isBlank()) {
                sql.append(" AND call_id = ?");
            }
            if (activityId != null && !activityId.isBlank()) {
                sql.append(" AND activity_id = ?");
            }
            this.params = buildParams(personId, partyRoleType, incidentId, callId, activityId);
        }

        private Object[] buildParams(String personId, String partyRoleType, String incidentId, String callId, String activityId) {
            int count = 0;
            if (personId != null && !personId.isBlank()) count++;
            if (partyRoleType != null && !partyRoleType.isBlank()) count++;
            if (incidentId != null && !incidentId.isBlank()) count++;
            if (callId != null && !callId.isBlank()) count++;
            if (activityId != null && !activityId.isBlank()) count++;
            
            if (count == 0) {
                return new Object[0];
            }
            
            Object[] result = new Object[count];
            int idx = 0;
            if (personId != null && !personId.isBlank()) {
                result[idx++] = personId;
            }
            if (partyRoleType != null && !partyRoleType.isBlank()) {
                result[idx++] = partyRoleType;
            }
            if (incidentId != null && !incidentId.isBlank()) {
                result[idx++] = incidentId;
            }
            if (callId != null && !callId.isBlank()) {
                result[idx++] = callId;
            }
            if (activityId != null && !activityId.isBlank()) {
                result[idx++] = activityId;
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
