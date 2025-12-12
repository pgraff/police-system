package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record AssignmentProjectionEntity(
        String assignmentId,
        Instant assignedTime,
        String assignmentType,
        String status,
        String incidentId,
        String callId,
        String dispatchId,
        Instant completedTime,
        Instant createdAt,
        Instant updatedAt) {
}

