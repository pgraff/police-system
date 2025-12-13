package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record VehicleProjectionResponse(
        String unitId,
        String vehicleType,
        String licensePlate,
        String vin,
        String status,
        String lastMaintenanceDate,
        Instant updatedAt) {
}
