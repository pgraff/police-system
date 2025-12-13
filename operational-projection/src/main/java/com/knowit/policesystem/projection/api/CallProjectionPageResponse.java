package com.knowit.policesystem.projection.api;

import java.util.List;

public record CallProjectionPageResponse(
        List<CallProjectionResponse> content,
        long total,
        int page,
        int size) {
}
