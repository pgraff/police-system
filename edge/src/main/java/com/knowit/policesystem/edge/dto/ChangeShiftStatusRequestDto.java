package com.knowit.policesystem.edge.dto;

import com.knowit.policesystem.edge.domain.ShiftStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for changing shift status.
 */
public class ChangeShiftStatusRequestDto {

    @NotNull(message = "status is required")
    private ShiftStatus status;

    public ChangeShiftStatusRequestDto() {
    }

    public ChangeShiftStatusRequestDto(ShiftStatus status) {
        this.status = status;
    }

    public ShiftStatus getStatus() {
        return status;
    }

    public void setStatus(ShiftStatus status) {
        this.status = status;
    }
}
