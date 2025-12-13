package com.knowit.policesystem.projection.api;

import java.util.List;

public record AssignmentWithResourcesResponse(
        AssignmentProjectionResponse assignment,
        List<ResourceAssignmentProjectionResponse> resourceAssignments) {
}
