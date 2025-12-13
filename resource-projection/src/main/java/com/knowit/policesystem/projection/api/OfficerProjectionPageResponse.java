package com.knowit.policesystem.projection.api;

import java.util.List;

public record OfficerProjectionPageResponse(
        List<OfficerProjectionResponse> content,
        long total,
        int page,
        int size) {
}
