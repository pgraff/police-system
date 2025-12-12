package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record CallStatusHistoryEntry(
        Long id,
        String callId,
        String status,
        Instant changedAt) {
}

