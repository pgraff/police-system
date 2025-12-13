package com.knowit.policesystem.projection.api;

import java.util.List;

public record UnitProjectionPageResponse(
        List<UnitProjectionResponse> content,
        long total,
        int page,
        int size) {
}
