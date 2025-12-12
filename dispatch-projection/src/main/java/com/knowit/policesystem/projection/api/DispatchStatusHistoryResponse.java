package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record DispatchStatusHistoryResponse(
        String status,
        Instant changedAt) {
}

