package com.knowit.policesystem.edge.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for changing call status.
 */
public class ChangeCallStatusRequestDto {

    @NotBlank(message = "status is required")
    private String status;

    public ChangeCallStatusRequestDto() {
    }

    public ChangeCallStatusRequestDto(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
