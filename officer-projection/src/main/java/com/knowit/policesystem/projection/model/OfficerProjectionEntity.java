package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record OfficerProjectionEntity(
        String badgeNumber,
        String firstName,
        String lastName,
        String rank,
        String email,
        String phoneNumber,
        String hireDate,
        String status,
        Instant updatedAt) {
}
