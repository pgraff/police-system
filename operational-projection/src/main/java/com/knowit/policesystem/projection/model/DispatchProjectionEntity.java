package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record DispatchProjectionEntity(
        String dispatchId,
        Instant dispatchTime,
        String dispatchType,
        String status,
        String callId,
        Instant updatedAt) {
}
