package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record OfficerShiftProjectionResponse(
        Long id,
        String shiftId,
        String badgeNumber,
        Instant checkInTime,
        Instant checkOutTime,
        String shiftRoleType,
        Instant updatedAt) {
}
