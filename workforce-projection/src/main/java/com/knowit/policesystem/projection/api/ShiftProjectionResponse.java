package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record ShiftProjectionResponse(
        String shiftId,
        Instant startTime,
        Instant endTime,
        String shiftType,
        String status,
        Instant updatedAt) {
}
