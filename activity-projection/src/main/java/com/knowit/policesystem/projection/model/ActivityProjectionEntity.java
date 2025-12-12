package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record ActivityProjectionEntity(
        String activityId,
        Instant activityTime,
        String activityType,
        String description,
        String status,
        Instant completedTime,
        Instant updatedAt) {
}

