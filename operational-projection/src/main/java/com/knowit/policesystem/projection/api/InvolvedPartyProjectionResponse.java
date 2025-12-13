package com.knowit.policesystem.projection.api;

import java.time.Instant;

public record InvolvedPartyProjectionResponse(
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
