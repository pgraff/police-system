package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record ShiftStatusHistoryEntry(
        String status,
        Instant changedAt) {
}
