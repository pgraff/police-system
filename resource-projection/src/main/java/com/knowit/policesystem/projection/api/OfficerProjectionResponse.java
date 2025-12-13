package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record OfficerProjectionResponse(
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
