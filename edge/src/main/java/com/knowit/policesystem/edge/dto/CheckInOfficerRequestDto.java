package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.knowit.policesystem.edge.domain.ShiftRoleType;
import com.knowit.policesystem.edge.util.FlexibleInstantDeserializer;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for checking in an officer to a shift.
 * Carries the check-in time and shift role type for an officer check-in request.
 */
public class CheckInOfficerRequestDto {

    @NotNull(message = "checkInTime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    @JsonDeserialize(using = FlexibleInstantDeserializer.class)
    private Instant checkInTime;

    @NotNull(message = "shiftRoleType is required")
    private ShiftRoleType shiftRoleType;

    /**
     * Default constructor for Jackson deserialization.
     */
    public CheckInOfficerRequestDto() {
    }

    /**
     * Creates a new check-in officer request DTO.
     *
     * @param checkInTime the check-in time
     * @param shiftRoleType the shift role type
     */
    public CheckInOfficerRequestDto(Instant checkInTime, ShiftRoleType shiftRoleType) {
        this.checkInTime = checkInTime;
        this.shiftRoleType = shiftRoleType;
    }

    public Instant getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(Instant checkInTime) {
        this.checkInTime = checkInTime;
    }

    public ShiftRoleType getShiftRoleType() {
        return shiftRoleType;
    }

    public void setShiftRoleType(ShiftRoleType shiftRoleType) {
        this.shiftRoleType = shiftRoleType;
    }
}
