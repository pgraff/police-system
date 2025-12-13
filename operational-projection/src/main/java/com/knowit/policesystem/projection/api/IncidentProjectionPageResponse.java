package com.knowit.policesystem.projection.api;

import java.util.List;

public record IncidentProjectionPageResponse(
        List<IncidentProjectionResponse> content,
        long total,
        int page,
        int size) {
}
