package com.knowit.policesystem.projection.repository;

import com.knowit.policesystem.common.events.vehicles.ChangeVehicleStatusRequested;
import com.knowit.policesystem.common.events.vehicles.RegisterVehicleRequested;
import com.knowit.policesystem.common.events.vehicles.UpdateVehicleRequested;
import com.knowit.policesystem.projection.model.VehicleProjectionEntity;
import com.knowit.policesystem.projection.model.VehicleStatusHistoryEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class VehicleProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public VehicleProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(RegisterVehicleRequested event, Instant updatedAt) {
        jdbcTemplate.update("""
                        INSERT INTO vehicle_projection (unit_id, vehicle_type, license_plate, vin, status, last_maintenance_date, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (unit_id) DO UPDATE SET
                            vehicle_type = EXCLUDED.vehicle_type,
                            license_plate = EXCLUDED.license_plate,
                            vin = EXCLUDED.vin,
                            status = EXCLUDED.status,
                            last_maintenance_date = EXCLUDED.last_maintenance_date,
                            updated_at = EXCLUDED.updated_at
                        """,
                event.getUnitId(),
                event.getVehicleType(),
                event.getLicensePlate(),
                event.getVin(),
                event.getStatus(),
                event.getLastMaintenanceDate(),
                ts(updatedAt));
    }

    public void applyUpdate(UpdateVehicleRequested event, Instant updatedAt) {
        VehicleProjectionEntity current = findByUnitId(event.getUnitId()).orElse(null);
        VehicleProjectionEntity merged = mergeUpdate(current, event, updatedAt);
        upsertMerged(merged);
    }

    public boolean changeStatus(ChangeVehicleStatusRequested event, Instant updatedAt) {
        VehicleProjectionEntity current = findByUnitId(event.getUnitId()).orElse(null);
        String currentStatus = current != null ? current.status() : null;
        String newStatus = event.getStatus();

        // Idempotent: only add history when status actually changes
        boolean statusChanged = newStatus != null && !newStatus.equals(currentStatus);

        VehicleProjectionEntity next = mergeStatus(current, event, updatedAt);
        upsertMerged(next);

        if (statusChanged) {
            jdbcTemplate.update("""
                    INSERT INTO vehicle_status_history (unit_id, status, changed_at)
                    VALUES (?, ?, ?)
                    """, event.getUnitId(), newStatus, ts(updatedAt));
        }

        return statusChanged;
    }

    public Optional<VehicleProjectionEntity> findByUnitId(String unitId) {
        List<VehicleProjectionEntity> result = jdbcTemplate.query("""
                SELECT unit_id, vehicle_type, license_plate, vin, status, last_maintenance_date, updated_at
                FROM vehicle_projection
                WHERE unit_id = ?
                """, projectionMapper(), unitId);
        return result.stream().findFirst();
    }

    public List<VehicleStatusHistoryEntry> findHistory(String unitId) {
        return jdbcTemplate.query("""
                SELECT status, changed_at
                FROM vehicle_status_history
                WHERE unit_id = ?
                ORDER BY changed_at
                """, historyMapper(), unitId);
    }

    public List<VehicleProjectionEntity> findAll(String status, String vehicleType, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT unit_id, vehicle_type, license_plate, vin, status, last_maintenance_date, updated_at
                FROM vehicle_projection
                WHERE 1=1
                """);
        QueryFilters filters = new QueryFilters(sql, status, vehicleType);
        sql.append(" ORDER BY unit_id LIMIT ? OFFSET ?");
        Object[] params = filters.withPaging(size, (long) page * size);
        return jdbcTemplate.query(sql.toString(), projectionMapper(), params);
    }

    public long count(String status, String vehicleType) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM vehicle_projection WHERE 1=1");
        QueryFilters filters = new QueryFilters(sql, status, vehicleType);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, filters.params());
    }

    private VehicleProjectionEntity mergeUpdate(VehicleProjectionEntity current, UpdateVehicleRequested event, Instant updatedAt) {
        return new VehicleProjectionEntity(
                event.getUnitId(),
                event.getVehicleType() != null ? event.getVehicleType() : current != null ? current.vehicleType() : null,
                event.getLicensePlate() != null ? event.getLicensePlate() : current != null ? current.licensePlate() : null,
                event.getVin() != null ? event.getVin() : current != null ? current.vin() : null,
                current != null ? current.status() : null,
                event.getLastMaintenanceDate() != null ? event.getLastMaintenanceDate() : current != null ? current.lastMaintenanceDate() : null,
                updatedAt
        );
    }

    private VehicleProjectionEntity mergeStatus(VehicleProjectionEntity current, ChangeVehicleStatusRequested event, Instant updatedAt) {
        return new VehicleProjectionEntity(
                event.getUnitId(),
                current != null ? current.vehicleType() : null,
                current != null ? current.licensePlate() : null,
                current != null ? current.vin() : null,
                event.getStatus(),
                current != null ? current.lastMaintenanceDate() : null,
                updatedAt
        );
    }

    private void upsertMerged(VehicleProjectionEntity entity) {
        jdbcTemplate.update("""
                        INSERT INTO vehicle_projection (unit_id, vehicle_type, license_plate, vin, status, last_maintenance_date, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (unit_id) DO UPDATE SET
                            vehicle_type = COALESCE(EXCLUDED.vehicle_type, vehicle_projection.vehicle_type),
                            license_plate = COALESCE(EXCLUDED.license_plate, vehicle_projection.license_plate),
                            vin = COALESCE(EXCLUDED.vin, vehicle_projection.vin),
                            status = COALESCE(EXCLUDED.status, vehicle_projection.status),
                            last_maintenance_date = COALESCE(EXCLUDED.last_maintenance_date, vehicle_projection.last_maintenance_date),
                            updated_at = EXCLUDED.updated_at
                        """,
                entity.unitId(),
                entity.vehicleType(),
                entity.licensePlate(),
                entity.vin(),
                entity.status(),
                entity.lastMaintenanceDate(),
                ts(entity.updatedAt()));
    }

    private RowMapper<VehicleProjectionEntity> projectionMapper() {
        return (rs, rowNum) -> mapProjection(rs);
    }

    private VehicleProjectionEntity mapProjection(ResultSet rs) throws SQLException {
        return new VehicleProjectionEntity(
                rs.getString("unit_id"),
                rs.getString("vehicle_type"),
                rs.getString("license_plate"),
                rs.getString("vin"),
                rs.getString("status"),
                rs.getString("last_maintenance_date"),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private RowMapper<VehicleStatusHistoryEntry> historyMapper() {
        return (rs, rowNum) -> mapHistory(rs);
    }

    private VehicleStatusHistoryEntry mapHistory(ResultSet rs) throws SQLException {
        return new VehicleStatusHistoryEntry(
                rs.getString("status"),
                rs.getTimestamp("changed_at").toInstant()
        );
    }

    private java.sql.Timestamp ts(Instant instant) {
        return instant != null ? java.sql.Timestamp.from(instant) : null;
    }

    private static class QueryFilters {
        private final Object[] params;

        QueryFilters(StringBuilder sql, String status, String vehicleType) {
            if (status != null && !status.isBlank()) {
                sql.append(" AND status = ?");
            }
            if (vehicleType != null && !vehicleType.isBlank()) {
                sql.append(" AND vehicle_type = ?");
            }
            this.params = buildParams(status, vehicleType);
        }

        private Object[] buildParams(String status, String vehicleType) {
            if ((status == null || status.isBlank()) && (vehicleType == null || vehicleType.isBlank())) {
                return new Object[0];
            }
            if (status != null && !status.isBlank() && vehicleType != null && !vehicleType.isBlank()) {
                return new Object[]{status, vehicleType};
            }
            if (status != null && !status.isBlank()) {
                return new Object[]{status};
            }
            return new Object[]{vehicleType};
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
