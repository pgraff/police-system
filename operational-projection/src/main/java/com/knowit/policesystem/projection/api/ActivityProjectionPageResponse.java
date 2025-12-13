package com.knowit.policesystem.projection.api;

import java.util.List;

public record ActivityProjectionPageResponse(
        List<ActivityProjectionResponse> content,
        long total,
        int page,
        int size) {
}
