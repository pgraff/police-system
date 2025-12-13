package com.knowit.policesystem.projection.api;

import java.util.List;

public record AssignmentProjectionPageResponse(
        List<AssignmentProjectionResponse> content,
        long total,
        int page,
        int size) {
}
