package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record ShiftStatusHistoryResponse(
        String status,
        Instant changedAt) {
}
