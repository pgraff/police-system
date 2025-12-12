package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record AssignmentStatusHistoryResponse(
        String status,
        Instant changedAt) {
}

