package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record IncidentProjectionResponse(
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
