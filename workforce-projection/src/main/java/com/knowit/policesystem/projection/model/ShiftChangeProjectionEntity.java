package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record ShiftChangeProjectionEntity(
        String shiftChangeId,
        String shiftId,
        Instant changeTime,
        String changeType,
        String notes,
        Instant updatedAt) {
}
