package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for clearing an incident.
 */
public class ClearIncidentRequestDto {

    @NotNull(message = "clearedTime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant clearedTime;

    /** Default constructor for Jackson deserialization. */
    public ClearIncidentRequestDto() {
    }

    /**
     * Creates a new clear-incident request DTO.
     *
     * @param clearedTime the time the incident was cleared
     */
    public ClearIncidentRequestDto(Instant clearedTime) {
        this.clearedTime = clearedTime;
    }

    public Instant getClearedTime() {
        return clearedTime;
    }

    public void setClearedTime(Instant clearedTime) {
        this.clearedTime = clearedTime;
    }
}
