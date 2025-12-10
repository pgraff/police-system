package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for unassigning a resource from an assignment.
 * Matches the UnassignResourceRequest schema in the OpenAPI specification.
 */
public class UnassignResourceRequestDto {

    @NotNull(message = "endTime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant endTime;

    /**
     * Default constructor for Jackson deserialization.
     */
    public UnassignResourceRequestDto() {
    }

    /**
     * Creates a new unassign resource request DTO.
     *
     * @param endTime the end time
     */
    public UnassignResourceRequestDto(Instant endTime) {
        this.endTime = endTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
}
