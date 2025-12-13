package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record DispatchProjectionResponse(
        String dispatchId,
        Instant dispatchTime,
        String dispatchType,
        String status,
        String callId,
        Instant updatedAt) {
}
