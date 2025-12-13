package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record ActivityProjectionResponse(
        String activityId,
        Instant activityTime,
        String activityType,
        String description,
        String status,
        Instant completedTime,
        String incidentId,
        Instant updatedAt) {
}
