package com.knowit.policesystem.edge.commands.shifts;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.EndShiftRequestDto;

import java.time.Instant;

/**
 * Command for ending a shift.
 * Processed by EndShiftCommandHandler.
 */
public class EndShiftCommand extends Command {

    private String shiftId;
    private Instant endTime;

    /**
     * Default constructor for deserialization.
     */
    public EndShiftCommand() {
        super();
    }

    /**
     * Creates a new end shift command from the request DTO.
     *
     * @param aggregateId the aggregate identifier (shiftId from path)
     * @param dto the request DTO containing end time
     */
    public EndShiftCommand(String aggregateId, EndShiftRequestDto dto) {
        super(aggregateId);
        this.shiftId = aggregateId;
        this.endTime = dto != null ? dto.getEndTime() : null;
    }

    @Override
    public String getCommandType() {
        return "EndShiftCommand";
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
}
