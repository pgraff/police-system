package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record CallProjectionEntity(
        String callId,
        String callNumber,
        String priority,
        String status,
        Instant receivedTime,
        Instant dispatchedTime,
        Instant arrivedTime,
        Instant clearedTime,
        String description,
        String callType,
        String incidentId,
        Instant updatedAt) {
}
