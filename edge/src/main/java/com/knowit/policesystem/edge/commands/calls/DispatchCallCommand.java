package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.DispatchCallRequestDto;

import java.time.Instant;

/**
 * Command for dispatching a call.
 */
public class DispatchCallCommand extends Command {

    private String callId;
    private Instant dispatchedTime;

    public DispatchCallCommand() {
        super();
    }

    public DispatchCallCommand(String aggregateId, DispatchCallRequestDto dto) {
        super(aggregateId);
        this.callId = aggregateId;
        this.dispatchedTime = dto.getDispatchedTime();
    }

    @Override
    public String getCommandType() {
        return "DispatchCallCommand";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public Instant getDispatchedTime() {
        return dispatchedTime;
    }

    public void setDispatchedTime(Instant dispatchedTime) {
        this.dispatchedTime = dispatchedTime;
    }
}
