package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for checking out an officer from a shift.
 * Carries the check-out time for an officer check-out request.
 */
public class CheckOutOfficerRequestDto {

    @NotNull(message = "checkOutTime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant checkOutTime;

    /**
     * Default constructor for Jackson deserialization.
     */
    public CheckOutOfficerRequestDto() {
    }

    /**
     * Creates a new check-out officer request DTO.
     *
     * @param checkOutTime the check-out time
     */
    public CheckOutOfficerRequestDto(Instant checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public Instant getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(Instant checkOutTime) {
        this.checkOutTime = checkOutTime;
    }
}
