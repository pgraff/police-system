package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for dispatching an incident.
 * Matches the DispatchIncidentRequest schema in the OpenAPI specification.
 */
public class DispatchIncidentRequestDto {

    @NotNull(message = "dispatchedTime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant dispatchedTime;

    /**
     * Default constructor for Jackson deserialization.
     */
    public DispatchIncidentRequestDto() {
    }

    /**
     * Creates a new dispatch incident request DTO.
     *
     * @param dispatchedTime the time the incident was dispatched
     */
    public DispatchIncidentRequestDto(Instant dispatchedTime) {
        this.dispatchedTime = dispatchedTime;
    }

    public Instant getDispatchedTime() {
        return dispatchedTime;
    }

    public void setDispatchedTime(Instant dispatchedTime) {
        this.dispatchedTime = dispatchedTime;
    }
}
