package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record ActivityStatusHistoryResponse(
        String status,
        Instant changedAt) {
}
