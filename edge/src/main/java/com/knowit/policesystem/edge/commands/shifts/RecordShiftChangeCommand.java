package com.knowit.policesystem.edge.commands.shifts;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.ChangeType;
import com.knowit.policesystem.edge.dto.RecordShiftChangeRequestDto;

import java.time.Instant;

/**
 * Command for recording a shift change.
 * This command is processed by RecordShiftChangeCommandHandler.
 */
public class RecordShiftChangeCommand extends Command {

    private String shiftId;
    private String shiftChangeId;
    private Instant changeTime;
    private ChangeType changeType;
    private String notes;

    /**
     * Default constructor for deserialization.
     */
    public RecordShiftChangeCommand() {
        super();
    }

    /**
     * Creates a new record shift change command from a DTO.
     *
     * @param aggregateId the aggregate identifier (shiftId)
     * @param dto the request DTO containing shift change data
     */
    public RecordShiftChangeCommand(String aggregateId, RecordShiftChangeRequestDto dto) {
        super(aggregateId);
        this.shiftId = aggregateId;
        this.shiftChangeId = dto.getShiftChangeId();
        this.changeTime = dto.getChangeTime();
        this.changeType = dto.getChangeType();
        this.notes = dto.getNotes();
    }

    @Override
    public String getCommandType() {
        return "RecordShiftChangeCommand";
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
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
