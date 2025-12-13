package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record UnitStatusHistoryResponse(
        String status,
        Instant changedAt) {
}
