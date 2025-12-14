package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.knowit.policesystem.edge.util.FlexibleInstantDeserializer;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for ending a shift.
 * Carries the end time for a shift end request.
 */
public class EndShiftRequestDto {

    @NotNull(message = "endTime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    @JsonDeserialize(using = FlexibleInstantDeserializer.class)
    private Instant endTime;

    /**
     * Default constructor for Jackson deserialization.
     */
    public EndShiftRequestDto() {
    }

    /**
     * Creates a new end shift request DTO.
     *
     * @param endTime the shift end time
     */
    public EndShiftRequestDto(Instant endTime) {
        this.endTime = endTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
}
