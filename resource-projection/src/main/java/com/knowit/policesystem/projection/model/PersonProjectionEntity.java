package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record PersonProjectionEntity(
        String personId,
        String firstName,
        String lastName,
        String dateOfBirth,
        String gender,
        String race,
        String phoneNumber,
        Instant updatedAt) {
}
