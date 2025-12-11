package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record OfficerStatusHistoryEntry(
        String status,
        Instant changedAt) {
}
