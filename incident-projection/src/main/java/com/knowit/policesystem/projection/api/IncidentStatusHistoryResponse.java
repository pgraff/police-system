package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record IncidentStatusHistoryResponse(
        String status,
        Instant changedAt) {
}
