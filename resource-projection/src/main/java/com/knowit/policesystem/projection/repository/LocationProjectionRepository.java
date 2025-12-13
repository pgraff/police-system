package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.locations.CreateLocationRequested;
import com.knowit.policesystem.common.events.locations.UpdateLocationRequested;
import com.knowit.policesystem.projection.model.LocationProjectionEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class LocationProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public LocationProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(CreateLocationRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        INSERT INTO location_projection (location_id, address, city, state, zip_code, latitude, longitude, location_type, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (location_id) DO UPDATE SET
                            address = EXCLUDED.address,
                            city = EXCLUDED.city,
                            state = EXCLUDED.state,
                            zip_code = EXCLUDED.zip_code,
                            latitude = EXCLUDED.latitude,
                            longitude = EXCLUDED.longitude,
                            location_type = EXCLUDED.location_type,
                            updated_at = EXCLUDED.updated_at
                        """,
                event.getLocationId(),
                event.getAddress(),
                event.getCity(),
                event.getState(),
                event.getZipCode(),
                event.getLatitude(),
                event.getLongitude(),
                event.getLocationType(),
                ts(updatedAt));
    }

    public void applyUpdate(UpdateLocationRequested event, Instant updatedAt) {
        LocationProjectionEntity current = findByLocationId(event.getLocationId()).orElse(null);
        LocationProjectionEntity merged = mergeUpdate(current, event, updatedAt);
        upsertMerged(merged);
    }

    public Optional<LocationProjectionEntity> findByLocationId(String locationId) {
        List<LocationProjectionEntity> result = jdbcTemplate.query("""
                SELECT location_id, address, city, state, zip_code, latitude, longitude, location_type, updated_at
                FROM location_projection
                WHERE location_id = ?
                """, projectionMapper(), locationId);
        return result.stream().findFirst();
    }

    public List<LocationProjectionEntity> findAll(String city, String state, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT location_id, address, city, state, zip_code, latitude, longitude, location_type, updated_at
                FROM location_projection
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, city, state);
        sql.append(" ORDER BY city, state LIMIT ? OFFSET ?");
        Object[] params = filters.withPaging(size, (long) page * size);
        return jdbcTemplate.query(sql.toString(), projectionMapper(), params);
    }

    public long count(String city, String state) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM location_projection WHERE 1=1");
        QueryFilters filters = new QueryFilters(sql, city, state);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, filters.params());
    }

    private LocationProjectionEntity mergeUpdate(LocationProjectionEntity current, UpdateLocationRequested event, Instant updatedAt) {
        return new LocationProjectionEntity(
                event.getLocationId(),
                event.getAddress() != null ? event.getAddress() : current != null ? current.address() : null,
                event.getCity() != null ? event.getCity() : current != null ? current.city() : null,
                event.getState() != null ? event.getState() : current != null ? current.state() : null,
                event.getZipCode() != null ? event.getZipCode() : current != null ? current.zipCode() : null,
                event.getLatitude() != null ? event.getLatitude() : current != null ? current.latitude() : null,
                event.getLongitude() != null ? event.getLongitude() : current != null ? current.longitude() : null,
                event.getLocationType() != null ? event.getLocationType() : current != null ? current.locationType() : null,
                updatedAt
        );
    }

    private void upsertMerged(LocationProjectionEntity entity) {
        jdbcTemplate.update("""
                        INSERT INTO location_projection (location_id, address, city, state, zip_code, latitude, longitude, location_type, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (location_id) DO UPDATE SET
                            address = COALESCE(EXCLUDED.address, location_projection.address),
                            city = COALESCE(EXCLUDED.city, location_projection.city),
                            state = COALESCE(EXCLUDED.state, location_projection.state),
                            zip_code = COALESCE(EXCLUDED.zip_code, location_projection.zip_code),
                            latitude = COALESCE(EXCLUDED.latitude, location_projection.latitude),
                            longitude = COALESCE(EXCLUDED.longitude, location_projection.longitude),
                            location_type = COALESCE(EXCLUDED.location_type, location_projection.location_type),
                            updated_at = EXCLUDED.updated_at
                        """,
                entity.locationId(),
                entity.address(),
                entity.city(),
                entity.state(),
                entity.zipCode(),
                entity.latitude(),
                entity.longitude(),
                entity.locationType(),
                ts(entity.updatedAt()));
    }

    private RowMapper<LocationProjectionEntity> projectionMapper() {
        return (rs, rowNum) -> mapProjection(rs);
    }

    private LocationProjectionEntity mapProjection(ResultSet rs) throws SQLException {
        return new LocationProjectionEntity(
                rs.getString("location_id"),
                rs.getString("address"),
                rs.getString("city"),
                rs.getString("state"),
                rs.getString("zip_code"),
                rs.getString("latitude"),
                rs.getString("longitude"),
                rs.getString("location_type"),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private java.sql.Timestamp ts(Instant instant) {
        return instant != null ? java.sql.Timestamp.from(instant) : null;
    }

    private static class QueryFilters {
        private final Object[] params;

        QueryFilters(StringBuilder sql, String city, String state) {
            if (city != null && !city.isBlank()) {
                sql.append(" AND city = ?");
            }
            if (state != null && !state.isBlank()) {
                sql.append(" AND state = ?");
            }
            this.params = buildParams(city, state);
        }

        private Object[] buildParams(String city, String state) {
            if ((city == null || city.isBlank()) && (state == null || state.isBlank())) {
                return new Object[0];
            }
            if (city != null && !city.isBlank() && state != null && !state.isBlank()) {
                return new Object[]{city, state};
            }
            if (city != null && !city.isBlank()) {
                return new Object[]{city};
            }
            return new Object[]{state};
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
