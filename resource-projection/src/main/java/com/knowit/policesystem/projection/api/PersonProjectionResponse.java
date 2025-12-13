package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record PersonProjectionResponse(
        String personId,
        String firstName,
        String lastName,
        String dateOfBirth,
        String gender,
        String race,
        String phoneNumber,
        Instant updatedAt) {
}
