package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record LocationProjectionEntity(
        String locationId,
        String address,
        String city,
        String state,
        String zipCode,
        String latitude,
        String longitude,
        String locationType,
        Instant updatedAt) {
}
