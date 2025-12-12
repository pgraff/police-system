package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record CallStatusHistoryResponse(
        String status,
        Instant changedAt) {
}

