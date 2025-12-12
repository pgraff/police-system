package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record AssignmentProjectionResponse(
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

