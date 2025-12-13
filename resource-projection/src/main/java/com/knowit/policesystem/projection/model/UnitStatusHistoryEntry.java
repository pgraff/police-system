package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record UnitStatusHistoryEntry(
        String status,
        Instant changedAt) {
}
