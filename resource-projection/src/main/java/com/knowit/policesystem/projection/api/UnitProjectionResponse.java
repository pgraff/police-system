package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record UnitProjectionResponse(
        String unitId,
        String unitType,
        String status,
        Instant updatedAt) {
}
