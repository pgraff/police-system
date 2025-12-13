package com.knowit.policesystem.projection.api;

import java.util.List;

public record ResourceAssignmentProjectionPageResponse(
        List<ResourceAssignmentProjectionResponse> content,
        long total,
        int page,
        int size) {
}
