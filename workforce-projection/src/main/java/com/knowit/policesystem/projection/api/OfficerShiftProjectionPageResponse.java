package com.knowit.policesystem.projection.api;

import java.util.List;

public record OfficerShiftProjectionPageResponse(
        List<OfficerShiftProjectionResponse> content,
        long total,
        int page,
        int size) {
}
