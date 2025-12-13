package com.knowit.policesystem.projection.model;

import java.time.Instant;

public record InvolvedPartyProjectionEntity(
        String involvementId,
        String personId,
        String partyRoleType,
        String description,
        Instant involvementStartTime,
        Instant involvementEndTime,
        String incidentId,
        String callId,
        String activityId,
        Instant updatedAt) {
}
