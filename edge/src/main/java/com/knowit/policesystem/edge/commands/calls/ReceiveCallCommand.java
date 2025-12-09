package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.CallStatus;
import com.knowit.policesystem.edge.domain.CallType;
import com.knowit.policesystem.edge.domain.Priority;
import com.knowit.policesystem.edge.dto.ReceiveCallRequestDto;

import java.time.Instant;

/**
 * Command for receiving a call.
 * This command is processed by ReceiveCallCommandHandler.
 */
public class ReceiveCallCommand extends Command {

    private String callId;
    private String callNumber;
    private Priority priority;
    private CallStatus status;
    private Instant receivedTime;
    private String description;
    private CallType callType;

    /**
     * Default constructor for deserialization.
     */
    public ReceiveCallCommand() {
        super();
    }

    /**
     * Creates a new receive call command from a DTO.
     *
     * @param aggregateId the aggregate identifier (callId)
     * @param dto the request DTO containing call data
     */
    public ReceiveCallCommand(String aggregateId, ReceiveCallRequestDto dto) {
        super(aggregateId);
        this.callId = dto.getCallId();
        this.callNumber = dto.getCallNumber();
        this.priority = dto.getPriority();
        this.status = dto.getStatus();
        this.receivedTime = dto.getReceivedTime();
        this.description = dto.getDescription();
        this.callType = dto.getCallType();
    }

    @Override
    public String getCommandType() {
        return "ReceiveCallCommand";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getCallNumber() {
        return callNumber;
    }

    public void setCallNumber(String callNumber) {
        this.callNumber = callNumber;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public CallStatus getStatus() {
        return status;
    }

    public void setStatus(CallStatus status) {
        this.status = status;
    }

    public Instant getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(Instant receivedTime) {
        this.receivedTime = receivedTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(CallType callType) {
        this.callType = callType;
    }
}
