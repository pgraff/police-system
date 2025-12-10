package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.knowit.policesystem.edge.domain.DispatchStatus;
import com.knowit.policesystem.edge.domain.DispatchType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for creating a dispatch.
 * Matches the CreateDispatchRequest schema in the OpenAPI specification.
 */
public class CreateDispatchRequestDto {

    @NotBlank(message = "dispatchId is required")
    private String dispatchId;

    @NotNull(message = "dispatchTime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant dispatchTime;

    @NotNull(message = "dispatchType is required")
    private DispatchType dispatchType;

    @NotNull(message = "status is required")
    private DispatchStatus status;

    /**
     * Default constructor for Jackson deserialization.
     */
    public CreateDispatchRequestDto() {
    }

    /**
     * Creates a new create dispatch request DTO.
     *
     * @param dispatchId the dispatch ID
     * @param dispatchTime the dispatch time
     * @param dispatchType the dispatch type
     * @param status the dispatch status
     */
    public CreateDispatchRequestDto(String dispatchId, Instant dispatchTime, DispatchType dispatchType, DispatchStatus status) {
        this.dispatchId = dispatchId;
        this.dispatchTime = dispatchTime;
        this.dispatchType = dispatchType;
        this.status = status;
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }

    public Instant getDispatchTime() {
        return dispatchTime;
    }

    public void setDispatchTime(Instant dispatchTime) {
        this.dispatchTime = dispatchTime;
    }

    public DispatchType getDispatchType() {
        return dispatchType;
    }

    public void setDispatchType(DispatchType dispatchType) {
        this.dispatchType = dispatchType;
    }

    public DispatchStatus getStatus() {
        return status;
    }

    public void setStatus(DispatchStatus status) {
        this.status = status;
    }
}
