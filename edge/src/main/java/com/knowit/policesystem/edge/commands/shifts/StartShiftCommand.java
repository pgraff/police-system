package com.knowit.policesystem.edge.commands.shifts;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.ShiftStatus;
import com.knowit.policesystem.edge.domain.ShiftType;
import com.knowit.policesystem.edge.dto.StartShiftRequestDto;

import java.time.Instant;

/**
 * Command for starting a shift.
 * This command is processed by StartShiftCommandHandler.
 */
public class StartShiftCommand extends Command {

    private String shiftId;
    private Instant startTime;
    private Instant endTime;
    private ShiftType shiftType;
    private ShiftStatus status;

    /**
     * Default constructor for deserialization.
     */
    public StartShiftCommand() {
        super();
    }

    /**
     * Creates a new start shift command from a DTO.
     *
     * @param aggregateId the aggregate identifier (shiftId)
     * @param dto the request DTO containing shift data
     */
    public StartShiftCommand(String aggregateId, StartShiftRequestDto dto) {
        super(aggregateId);
        this.shiftId = dto.getShiftId();
        this.startTime = dto.getStartTime();
        this.endTime = dto.getEndTime();
        this.shiftType = dto.getShiftType();
        this.status = dto.getStatus();
    }

    @Override
    public String getCommandType() {
        return "StartShiftCommand";
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
