package com.knowit.policesystem.edge.dto;

import com.knowit.policesystem.edge.domain.ShiftRoleType;

/**
 * Request DTO for updating an officer shift.
 * Carries the shift role type for an officer shift update request.
 * At least one field must be provided.
 */
public class UpdateOfficerShiftRequestDto {

    private ShiftRoleType shiftRoleType;

    /**
     * Default constructor for Jackson deserialization.
     */
    public UpdateOfficerShiftRequestDto() {
    }

    /**
     * Creates a new update officer shift request DTO.
     *
     * @param shiftRoleType the shift role type (optional)
     */
    public UpdateOfficerShiftRequestDto(ShiftRoleType shiftRoleType) {
        this.shiftRoleType = shiftRoleType;
    }

    public ShiftRoleType getShiftRoleType() {
        return shiftRoleType;
    }

    public void setShiftRoleType(ShiftRoleType shiftRoleType) {
        this.shiftRoleType = shiftRoleType;
    }
}
