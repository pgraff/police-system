package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record DispatchStatusHistoryEntry(
        Long id,
        String dispatchId,
        String status,
        Instant changedAt) {
}

