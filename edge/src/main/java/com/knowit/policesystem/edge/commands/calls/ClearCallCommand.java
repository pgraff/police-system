package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.ClearCallRequestDto;

import java.time.Instant;

/**
 * Command for clearing a call.
 */
public class ClearCallCommand extends Command {

    private String callId;
    private Instant clearedTime;

    public ClearCallCommand() {
        super();
    }

    public ClearCallCommand(String aggregateId, ClearCallRequestDto dto) {
        super(aggregateId);
        this.callId = aggregateId;
        this.clearedTime = dto.getClearedTime();
    }

    @Override
    public String getCommandType() {
        return "ClearCallCommand";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public Instant getClearedTime() {
        return clearedTime;
    }

    public void setClearedTime(Instant clearedTime) {
        this.clearedTime = clearedTime;
    }
}
