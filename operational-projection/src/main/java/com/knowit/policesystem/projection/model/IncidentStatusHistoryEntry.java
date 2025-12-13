package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record IncidentStatusHistoryEntry(
        Long id,
        String incidentId,
        String status,
        Instant changedAt) {
}
