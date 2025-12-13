package com.knowit.policesystem.projection.api;

import java.util.List;

public record CallWithDispatchesResponse(
        CallProjectionResponse call,
        List<DispatchProjectionResponse> dispatches) {
}
