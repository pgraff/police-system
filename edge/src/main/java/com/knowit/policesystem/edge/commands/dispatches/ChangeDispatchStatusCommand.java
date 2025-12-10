package com.knowit.policesystem.edge.commands.dispatches;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.DispatchStatus;
import com.knowit.policesystem.edge.dto.ChangeDispatchStatusRequestDto;

/**
 * Command for changing a dispatch's status.
 * This command is processed by ChangeDispatchStatusCommandHandler.
 */
public class ChangeDispatchStatusCommand extends Command {

    private String dispatchId;
    private DispatchStatus status;

    /**
     * Default constructor for deserialization.
     */
    public ChangeDispatchStatusCommand() {
        super();
    }

    /**
     * Creates a new change dispatch status command from a DTO.
     *
     * @param aggregateId the aggregate identifier (dispatchId)
     * @param dto the request DTO containing status data
     */
    public ChangeDispatchStatusCommand(String aggregateId, ChangeDispatchStatusRequestDto dto) {
        super(aggregateId);
        this.dispatchId = aggregateId;
        this.status = dto.getStatus();
    }

    @Override
    public String getCommandType() {
        return "ChangeDispatchStatusCommand";
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }

    public DispatchStatus getStatus() {
        return status;
    }

    public void setStatus(DispatchStatus status) {
        this.status = status;
    }
}
