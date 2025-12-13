package com.knowit.policesystem.projection.api;

import java.util.List;

public record ShiftProjectionPageResponse(
        List<ShiftProjectionResponse> content,
        long total,
        int page,
        int size) {
}
