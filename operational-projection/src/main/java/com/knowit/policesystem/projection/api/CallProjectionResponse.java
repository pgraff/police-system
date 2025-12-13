package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record CallProjectionResponse(
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
