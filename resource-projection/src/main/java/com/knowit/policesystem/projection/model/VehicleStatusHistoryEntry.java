package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record VehicleStatusHistoryEntry(
        String status,
        Instant changedAt) {
}
