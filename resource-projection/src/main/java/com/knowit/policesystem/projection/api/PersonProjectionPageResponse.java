package com.knowit.policesystem.projection.api;

import java.util.List;

public record PersonProjectionPageResponse(
        List<PersonProjectionResponse> content,
        long total,
        int page,
        int size) {
}
