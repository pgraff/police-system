package com.knowit.policesystem.projection.api;

import java.util.List;

public record VehicleProjectionPageResponse(
        List<VehicleProjectionResponse> content,
        long total,
        int page,
        int size) {
}
