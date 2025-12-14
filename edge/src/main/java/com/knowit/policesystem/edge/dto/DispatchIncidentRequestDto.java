package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.knowit.policesystem.edge.util.FlexibleInstantDeserializer;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for dispatching an incident.
 * Matches the DispatchIncidentRequest schema in the OpenAPI specification.
 */
public class DispatchIncidentRequestDto {

    @NotNull(message = "dispatchedTime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.knowit.policesystem.edge.util.FlexibleInstantDeserializer.class)
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
