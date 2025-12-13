package com.knowit.policesystem.projection.api;

import java.util.List;

public record InvolvedPartyProjectionPageResponse(
        List<InvolvedPartyProjectionResponse> content,
        long total,
        int page,
        int size) {
}
