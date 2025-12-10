package com.knowit.policesystem.edge.dto;

import com.knowit.policesystem.edge.domain.AssignmentStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for changing assignment status.
 */
public class ChangeAssignmentStatusRequestDto {

    @NotNull(message = "status is required")
    private AssignmentStatus status;

    public ChangeAssignmentStatusRequestDto() {
    }

    public ChangeAssignmentStatusRequestDto(AssignmentStatus status) {
        this.status = status;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }
}
