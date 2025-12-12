package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record AssignmentResourceEntry(
        Long id,
        String assignmentId,
        String resourceId,
        String resourceType,
        String roleType,
        String status,
        Instant startTime,
        Instant updatedAt) {
}

