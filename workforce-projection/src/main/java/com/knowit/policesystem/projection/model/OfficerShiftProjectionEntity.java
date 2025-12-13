package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record OfficerShiftProjectionEntity(
        Long id,
        String shiftId,
        String badgeNumber,
        Instant checkInTime,
        Instant checkOutTime,
        String shiftRoleType,
        Instant updatedAt) {
}
