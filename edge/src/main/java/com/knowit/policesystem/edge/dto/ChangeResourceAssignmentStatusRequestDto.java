package com.knowit.policesystem.edge.dto;

import com.knowit.policesystem.edge.domain.ResourceAssignmentStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for changing resource assignment status.
 */
public class ChangeResourceAssignmentStatusRequestDto {

    @NotNull(message = "status is required")
    private ResourceAssignmentStatus status;

    public ChangeResourceAssignmentStatusRequestDto() {
    }

    public ChangeResourceAssignmentStatusRequestDto(ResourceAssignmentStatus status) {
        this.status = status;
    }

    public ResourceAssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceAssignmentStatus status) {
        this.status = status;
    }
}
