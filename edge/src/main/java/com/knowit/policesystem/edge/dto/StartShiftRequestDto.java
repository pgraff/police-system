package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.knowit.policesystem.edge.domain.ShiftStatus;
import com.knowit.policesystem.edge.domain.ShiftType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for starting a shift.
 * Matches the StartShiftRequest schema in the OpenAPI specification.
 */
public class StartShiftRequestDto {

    @NotBlank(message = "shiftId is required")
    private String shiftId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant endTime;

    @NotNull(message = "shiftType is required")
    private ShiftType shiftType;

    @NotNull(message = "status is required")
    private ShiftStatus status;

    /**
     * Default constructor for Jackson deserialization.
     */
    public StartShiftRequestDto() {
    }

    /**
     * Creates a new start shift request DTO.
     *
     * @param shiftId the shift identifier
     * @param startTime the optional shift start time
     * @param endTime the optional shift end time
     * @param shiftType the shift type
     * @param status the shift status
     */
    public StartShiftRequestDto(String shiftId, Instant startTime, Instant endTime, ShiftType shiftType, ShiftStatus status) {
        this.shiftId = shiftId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.shiftType = shiftType;
        this.status = status;
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public ShiftType getShiftType() {
        return shiftType;
    }

    public void setShiftType(ShiftType shiftType) {
        this.shiftType = shiftType;
    }

    public ShiftStatus getStatus() {
        return status;
    }

    public void setStatus(ShiftStatus status) {
        this.status = status;
    }
}
