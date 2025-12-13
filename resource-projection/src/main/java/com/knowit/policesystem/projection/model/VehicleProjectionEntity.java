package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record VehicleProjectionEntity(
        String unitId,
        String vehicleType,
        String licensePlate,
        String vin,
        String status,
        String lastMaintenanceDate,
        Instant updatedAt) {
}
