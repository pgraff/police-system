package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.persons.RegisterPersonRequested;
import com.knowit.policesystem.common.events.persons.UpdatePersonRequested;
import com.knowit.policesystem.projection.model.PersonProjectionEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class PersonProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public PersonProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(RegisterPersonRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        INSERT INTO person_projection (person_id, first_name, last_name, date_of_birth, gender, race, phone_number, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (person_id) DO UPDATE SET
                            first_name = EXCLUDED.first_name,
                            last_name = EXCLUDED.last_name,
                            date_of_birth = EXCLUDED.date_of_birth,
                            gender = EXCLUDED.gender,
                            race = EXCLUDED.race,
                            phone_number = EXCLUDED.phone_number,
                            updated_at = EXCLUDED.updated_at
                        """,
                event.getPersonId(),
                event.getFirstName(),
                event.getLastName(),
                event.getDateOfBirth(),
                event.getGender(),
                event.getRace(),
                event.getPhoneNumber(),
                ts(updatedAt));
    }

    public void applyUpdate(UpdatePersonRequested event, Instant updatedAt) {
        PersonProjectionEntity current = findByPersonId(event.getPersonId()).orElse(null);
        PersonProjectionEntity merged = mergeUpdate(current, event, updatedAt);
        upsertMerged(merged);
    }

    public Optional<PersonProjectionEntity> findByPersonId(String personId) {
        List<PersonProjectionEntity> result = jdbcTemplate.query("""
                SELECT person_id, first_name, last_name, date_of_birth, gender, race, phone_number, updated_at
                FROM person_projection
                WHERE person_id = ?
                """, projectionMapper(), personId);
        return result.stream().findFirst();
    }

    public List<PersonProjectionEntity> findAll(String lastName, String firstName, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT person_id, first_name, last_name, date_of_birth, gender, race, phone_number, updated_at
                FROM person_projection
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, lastName, firstName);
        sql.append(" ORDER BY last_name, first_name LIMIT ? OFFSET ?");
        Object[] params = filters.withPaging(size, (long) page * size);
        return jdbcTemplate.query(sql.toString(), projectionMapper(), params);
    }

    public long count(String lastName, String firstName) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM person_projection WHERE 1=1");
        QueryFilters filters = new QueryFilters(sql, lastName, firstName);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, filters.params());
    }

    private PersonProjectionEntity mergeUpdate(PersonProjectionEntity current, UpdatePersonRequested event, Instant updatedAt) {
        return new PersonProjectionEntity(
                event.getPersonId(),
                event.getFirstName() != null ? event.getFirstName() : current != null ? current.firstName() : null,
                event.getLastName() != null ? event.getLastName() : current != null ? current.lastName() : null,
                event.getDateOfBirth() != null ? event.getDateOfBirth() : current != null ? current.dateOfBirth() : null,
                event.getGender() != null ? event.getGender() : current != null ? current.gender() : null,
                event.getRace() != null ? event.getRace() : current != null ? current.race() : null,
                event.getPhoneNumber() != null ? event.getPhoneNumber() : current != null ? current.phoneNumber() : null,
                updatedAt
        );
    }

    private void upsertMerged(PersonProjectionEntity entity) {
        jdbcTemplate.update("""
                        INSERT INTO person_projection (person_id, first_name, last_name, date_of_birth, gender, race, phone_number, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (person_id) DO UPDATE SET
                            first_name = COALESCE(EXCLUDED.first_name, person_projection.first_name),
                            last_name = COALESCE(EXCLUDED.last_name, person_projection.last_name),
                            date_of_birth = COALESCE(EXCLUDED.date_of_birth, person_projection.date_of_birth),
                            gender = COALESCE(EXCLUDED.gender, person_projection.gender),
                            race = COALESCE(EXCLUDED.race, person_projection.race),
                            phone_number = COALESCE(EXCLUDED.phone_number, person_projection.phone_number),
                            updated_at = EXCLUDED.updated_at
                        """,
                entity.personId(),
                entity.firstName(),
                entity.lastName(),
                entity.dateOfBirth(),
                entity.gender(),
                entity.race(),
                entity.phoneNumber(),
                ts(entity.updatedAt()));
    }

    private RowMapper<PersonProjectionEntity> projectionMapper() {
        return (rs, rowNum) -> mapProjection(rs);
    }

    private PersonProjectionEntity mapProjection(ResultSet rs) throws SQLException {
        return new PersonProjectionEntity(
                rs.getString("person_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("date_of_birth"),
                rs.getString("gender"),
                rs.getString("race"),
                rs.getString("phone_number"),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private java.sql.Timestamp ts(Instant instant) {
        return instant != null ? java.sql.Timestamp.from(instant) : null;
    }

    private static class QueryFilters {
        private final Object[] params;

        QueryFilters(StringBuilder sql, String lastName, String firstName) {
            if (lastName != null && !lastName.isBlank()) {
                sql.append(" AND last_name = ?");
            }
            if (firstName != null && !firstName.isBlank()) {
                sql.append(" AND first_name = ?");
            }
            this.params = buildParams(lastName, firstName);
        }

        private Object[] buildParams(String lastName, String firstName) {
            if ((lastName == null || lastName.isBlank()) && (firstName == null || firstName.isBlank())) {
                return new Object[0];
            }
            if (lastName != null && !lastName.isBlank() && firstName != null && !firstName.isBlank()) {
                return new Object[]{lastName, firstName};
            }
            if (lastName != null && !lastName.isBlank()) {
                return new Object[]{lastName};
            }
            return new Object[]{firstName};
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
