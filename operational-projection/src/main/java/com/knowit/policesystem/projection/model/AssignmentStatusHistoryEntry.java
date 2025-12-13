package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record AssignmentStatusHistoryEntry(
        Long id,
        String assignmentId,
        String status,
        Instant changedAt) {
}
