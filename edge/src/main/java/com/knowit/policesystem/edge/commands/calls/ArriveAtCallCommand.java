package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.ArriveAtCallRequestDto;

import java.time.Instant;

/**
 * Command for arriving at a call.
 */
public class ArriveAtCallCommand extends Command {

    private String callId;
    private Instant arrivedTime;

    public ArriveAtCallCommand() {
        super();
    }

    public ArriveAtCallCommand(String aggregateId, ArriveAtCallRequestDto dto) {
        super(aggregateId);
        this.callId = aggregateId;
        this.arrivedTime = dto.getArrivedTime();
    }

    @Override
    public String getCommandType() {
        return "ArriveAtCallCommand";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public Instant getArrivedTime() {
        return arrivedTime;
    }

    public void setArrivedTime(Instant arrivedTime) {
        this.arrivedTime = arrivedTime;
    }
}
