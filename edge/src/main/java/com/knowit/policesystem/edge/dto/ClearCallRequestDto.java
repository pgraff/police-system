package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for clearing a call.
 */
public class ClearCallRequestDto {

    @NotNull(message = "clearedTime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant clearedTime;

    public ClearCallRequestDto() {
    }

    public ClearCallRequestDto(Instant clearedTime) {
        this.clearedTime = clearedTime;
    }

    public Instant getClearedTime() {
        return clearedTime;
    }

    public void setClearedTime(Instant clearedTime) {
        this.clearedTime = clearedTime;
    }
}
