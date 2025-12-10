package com.knowit.policesystem.edge.commands.dispatches;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.DispatchStatus;
import com.knowit.policesystem.edge.domain.DispatchType;
import com.knowit.policesystem.edge.dto.CreateDispatchRequestDto;

import java.time.Instant;

/**
 * Command for creating a dispatch.
 * This command is processed by CreateDispatchCommandHandler.
 */
public class CreateDispatchCommand extends Command {

    private String dispatchId;
    private Instant dispatchTime;
    private DispatchType dispatchType;
    private DispatchStatus status;

    /**
     * Default constructor for deserialization.
     */
    public CreateDispatchCommand() {
        super();
    }

    /**
     * Creates a new create dispatch command from a DTO.
     *
     * @param aggregateId the aggregate identifier (dispatchId)
     * @param dto the request DTO containing dispatch data
     */
    public CreateDispatchCommand(String aggregateId, CreateDispatchRequestDto dto) {
        super(aggregateId);
        this.dispatchId = dto.getDispatchId();
        this.dispatchTime = dto.getDispatchTime();
        this.dispatchType = dto.getDispatchType();
        this.status = dto.getStatus();
    }

    @Override
    public String getCommandType() {
        return "CreateDispatchCommand";
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
