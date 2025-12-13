package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record VehicleStatusHistoryResponse(
        String status,
        Instant changedAt) {
}
