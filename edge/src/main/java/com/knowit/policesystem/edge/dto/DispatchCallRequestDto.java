package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for dispatching a call.
 */
public class DispatchCallRequestDto {

    @NotNull(message = "dispatchedTime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant dispatchedTime;

    public DispatchCallRequestDto() {
    }

    public DispatchCallRequestDto(Instant dispatchedTime) {
        this.dispatchedTime = dispatchedTime;
    }

    public Instant getDispatchedTime() {
        return dispatchedTime;
    }

    public void setDispatchedTime(Instant dispatchedTime) {
        this.dispatchedTime = dispatchedTime;
    }
}
