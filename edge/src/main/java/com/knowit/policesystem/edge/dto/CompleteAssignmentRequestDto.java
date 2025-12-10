package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for completing an assignment.
 */
public class CompleteAssignmentRequestDto {

    @NotNull(message = "completedTime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant completedTime;

    public CompleteAssignmentRequestDto() {
    }

    public CompleteAssignmentRequestDto(Instant completedTime) {
        this.completedTime = completedTime;
    }

    public Instant getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(Instant completedTime) {
        this.completedTime = completedTime;
    }
}
