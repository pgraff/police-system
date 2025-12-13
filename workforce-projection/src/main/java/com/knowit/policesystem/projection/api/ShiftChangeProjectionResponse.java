package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record ShiftChangeProjectionResponse(
        String shiftChangeId,
        String shiftId,
        Instant changeTime,
        String changeType,
        String notes,
        Instant updatedAt) {
}
