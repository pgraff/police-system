package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record ShiftProjectionEntity(
        String shiftId,
        Instant startTime,
        Instant endTime,
        String shiftType,
        String status,
        Instant updatedAt) {
}
