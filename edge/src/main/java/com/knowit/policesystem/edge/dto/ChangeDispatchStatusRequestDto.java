package com.knowit.policesystem.edge.dto;

import com.knowit.policesystem.edge.domain.DispatchStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for changing dispatch status.
 */
public class ChangeDispatchStatusRequestDto {

    @NotNull(message = "status is required")
    private DispatchStatus status;

    public ChangeDispatchStatusRequestDto() {
    }

    public ChangeDispatchStatusRequestDto(DispatchStatus status) {
        this.status = status;
    }

    public DispatchStatus getStatus() {
        return status;
    }

    public void setStatus(DispatchStatus status) {
        this.status = status;
    }
}
