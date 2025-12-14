package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.knowit.policesystem.edge.domain.ChangeType;
import com.knowit.policesystem.edge.util.FlexibleInstantDeserializer;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for recording a shift change.
 * Matches the RecordShiftChangeRequest schema in the OpenAPI specification.
 */
public class RecordShiftChangeRequestDto {

    @NotNull(message = "shiftChangeId is required")
    private String shiftChangeId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    @JsonDeserialize(using = FlexibleInstantDeserializer.class)
    private Instant changeTime;

    @NotNull(message = "changeType is required")
    private ChangeType changeType;

    private String notes;

    /**
     * Default constructor for Jackson deserialization.
     */
    public RecordShiftChangeRequestDto() {
    }

    /**
     * Creates a new record shift change request DTO.
     *
     * @param shiftChangeId the shift change identifier
     * @param changeTime the change time
     * @param changeType the change type
     * @param notes optional notes
     */
    public RecordShiftChangeRequestDto(String shiftChangeId, Instant changeTime, ChangeType changeType, String notes) {
        this.shiftChangeId = shiftChangeId;
        this.changeTime = changeTime;
        this.changeType = changeType;
        this.notes = notes;
    }

    public String getShiftChangeId() {
        return shiftChangeId;
    }

    public void setShiftChangeId(String shiftChangeId) {
        this.shiftChangeId = shiftChangeId;
    }

    public Instant getChangeTime() {
        return changeTime;
    }

    public void setChangeTime(Instant changeTime) {
        this.changeTime = changeTime;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
