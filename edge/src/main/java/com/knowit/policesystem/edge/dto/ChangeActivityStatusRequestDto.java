package com.knowit.policesystem.edge.dto;

import com.knowit.policesystem.edge.domain.ActivityStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for changing activity status.
 */
public class ChangeActivityStatusRequestDto {

    @NotNull(message = "status is required")
    private ActivityStatus status;

    public ChangeActivityStatusRequestDto() {
    }

    public ChangeActivityStatusRequestDto(ActivityStatus status) {
        this.status = status;
    }

    public ActivityStatus getStatus() {
        return status;
    }

    public void setStatus(ActivityStatus status) {
        this.status = status;
    }
}
