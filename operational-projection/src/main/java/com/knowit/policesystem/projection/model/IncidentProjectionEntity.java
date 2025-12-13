package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record IncidentProjectionEntity(
        String incidentId,
        String incidentNumber,
        String priority,
        String status,
        Instant reportedTime,
        Instant dispatchedTime,
        Instant arrivedTime,
        Instant clearedTime,
        String description,
        String incidentType,
        Instant updatedAt) {
}
