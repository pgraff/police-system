package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record ResourceAssignmentProjectionResponse(
        Long id,
        String assignmentId,
        String resourceId,
        String resourceType,
        String roleType,
        String status,
        Instant startTime,
        Instant endTime,
        Instant updatedAt) {
}
