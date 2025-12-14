package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.knowit.policesystem.edge.util.FlexibleInstantDeserializer;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for ending a party involvement.
 * Carries the end time for a party involvement end request.
 */
public class EndPartyInvolvementRequestDto {

    @NotNull(message = "involvementEndTime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    @JsonDeserialize(using = FlexibleInstantDeserializer.class)
    private Instant involvementEndTime;

    /**
     * Default constructor for Jackson deserialization.
     */
    public EndPartyInvolvementRequestDto() {
    }

    /**
     * Creates a new end party involvement request DTO.
     *
     * @param involvementEndTime the involvement end time
     */
    public EndPartyInvolvementRequestDto(Instant involvementEndTime) {
        this.involvementEndTime = involvementEndTime;
    }

    public Instant getInvolvementEndTime() {
        return involvementEndTime;
    }

    public void setInvolvementEndTime(Instant involvementEndTime) {
        this.involvementEndTime = involvementEndTime;
    }
}
