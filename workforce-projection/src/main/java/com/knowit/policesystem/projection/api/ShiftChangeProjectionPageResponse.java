package com.knowit.policesystem.projection.api;

import java.util.List;

public record ShiftChangeProjectionPageResponse(
        List<ShiftChangeProjectionResponse> content,
        long total,
        int page,
        int size) {
}
