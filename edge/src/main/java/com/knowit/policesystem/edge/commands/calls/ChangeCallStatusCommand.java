package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.ChangeCallStatusRequestDto;

/**
 * Command for changing a call's status.
 */
public class ChangeCallStatusCommand extends Command {

    private String callId;
    private String status;

    public ChangeCallStatusCommand() {
        super();
    }

    public ChangeCallStatusCommand(String aggregateId, ChangeCallStatusRequestDto dto) {
        super(aggregateId);
        this.callId = aggregateId;
        this.status = dto.getStatus();
    }

    @Override
    public String getCommandType() {
        return "ChangeCallStatusCommand";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
