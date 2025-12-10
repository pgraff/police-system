package com.knowit.policesystem.edge.commands.shifts;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.ShiftStatus;
import com.knowit.policesystem.edge.dto.ChangeShiftStatusRequestDto;

/**
 * Command for changing a shift's status.
 * This command is processed by ChangeShiftStatusCommandHandler.
 */
public class ChangeShiftStatusCommand extends Command {

    private String shiftId;
    private ShiftStatus status;

    /**
     * Default constructor for deserialization.
     */
    public ChangeShiftStatusCommand() {
        super();
    }

    /**
     * Creates a new change shift status command from a DTO.
     *
     * @param aggregateId the aggregate identifier (shiftId)
     * @param dto the request DTO containing status data
     */
    public ChangeShiftStatusCommand(String aggregateId, ChangeShiftStatusRequestDto dto) {
        super(aggregateId);
        this.shiftId = aggregateId;
        this.status = dto.getStatus();
    }

    @Override
    public String getCommandType() {
        return "ChangeShiftStatusCommand";
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public ShiftStatus getStatus() {
        return status;
    }

    public void setStatus(ShiftStatus status) {
        this.status = status;
    }
}
