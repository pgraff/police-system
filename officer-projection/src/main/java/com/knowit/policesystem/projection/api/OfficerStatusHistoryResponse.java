package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record OfficerStatusHistoryResponse(
        String status,
        Instant changedAt) {
}
