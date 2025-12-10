package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for arriving at a call.
 */
public class ArriveAtCallRequestDto {

    @NotNull(message = "arrivedTime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant arrivedTime;

    public ArriveAtCallRequestDto() {
    }

    public ArriveAtCallRequestDto(Instant arrivedTime) {
        this.arrivedTime = arrivedTime;
    }

    public Instant getArrivedTime() {
        return arrivedTime;
    }

    public void setArrivedTime(Instant arrivedTime) {
        this.arrivedTime = arrivedTime;
    }
}
