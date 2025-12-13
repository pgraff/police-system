package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record LocationProjectionResponse(
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
