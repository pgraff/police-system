package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record ActivityStatusHistoryEntry(
        Long id,
        String activityId,
        String status,
        Instant changedAt) {
}
