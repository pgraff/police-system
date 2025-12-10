package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.CallType;
import com.knowit.policesystem.edge.domain.Priority;
import com.knowit.policesystem.edge.dto.UpdateCallRequestDto;

/**
 * Command for updating a call.
 */
public class UpdateCallCommand extends Command {

    private String callId;
    private Priority priority;
    private String description;
    private CallType callType;

    public UpdateCallCommand() {
        super();
    }

    public UpdateCallCommand(String aggregateId, UpdateCallRequestDto dto) {
        super(aggregateId);
        this.callId = aggregateId;
        this.priority = dto.getPriority();
        this.description = dto.getDescription();
        this.callType = dto.getCallType();
    }

    @Override
    public String getCommandType() {
        return "UpdateCallCommand";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
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
