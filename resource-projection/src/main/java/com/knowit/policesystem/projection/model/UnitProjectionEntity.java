package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record UnitProjectionEntity(
        String unitId,
        String unitType,
        String status,
        Instant updatedAt) {
}
