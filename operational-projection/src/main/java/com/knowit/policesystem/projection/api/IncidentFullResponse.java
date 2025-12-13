package com.knowit.policesystem.projection.api;

import java.util.List;

public record IncidentFullResponse(
        IncidentProjectionResponse incident,
        List<CallWithDispatchesResponse> calls,
        List<ActivityProjectionResponse> activities,
        List<AssignmentWithResourcesResponse> assignments,
        List<InvolvedPartyProjectionResponse> involvedParties) {
}
