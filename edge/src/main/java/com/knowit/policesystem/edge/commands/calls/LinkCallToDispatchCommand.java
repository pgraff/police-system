package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.LinkCallToDispatchRequestDto;

/**
 * Command for linking a call to a dispatch.
 */
public class LinkCallToDispatchCommand extends Command {

    private String callId;
    private String dispatchId;

    /**
     * Default constructor for deserialization.
     */
    public LinkCallToDispatchCommand() {
        super();
    }

    /**
     * Creates a new link call to dispatch command.
     *
     * @param aggregateId the aggregate identifier (callId)
     * @param callId the call ID from the path
     * @param dto the request DTO containing dispatch link data
     */
    public LinkCallToDispatchCommand(String aggregateId, String callId, LinkCallToDispatchRequestDto dto) {
        super(aggregateId);
        this.callId = callId;
        this.dispatchId = dto.getDispatchId();
    }

    @Override
    public String getCommandType() {
        return "LinkCallToDispatchCommand";
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }
}
