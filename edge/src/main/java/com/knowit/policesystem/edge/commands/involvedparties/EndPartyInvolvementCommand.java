package com.knowit.policesystem.edge.commands.involvedparties;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.dto.EndPartyInvolvementRequestDto;

import java.time.Instant;

/**
 * Command for ending a party involvement.
 * Processed by EndPartyInvolvementCommandHandler.
 */
public class EndPartyInvolvementCommand extends Command {

    private String involvementId;
    private Instant involvementEndTime;

    /**
     * Default constructor for deserialization.
     */
    public EndPartyInvolvementCommand() {
        super();
    }

    /**
     * Creates a new end party involvement command from the request DTO.
     *
     * @param aggregateId the aggregate identifier (involvementId from path)
     * @param dto the request DTO containing end time
     */
    public EndPartyInvolvementCommand(String aggregateId, EndPartyInvolvementRequestDto dto) {
        super(aggregateId);
        this.involvementId = aggregateId;
        this.involvementEndTime = dto != null ? dto.getInvolvementEndTime() : null;
    }

    @Override
    public String getCommandType() {
        return "EndPartyInvolvementCommand";
    }

    public String getInvolvementId() {
        return involvementId;
    }

    public void setInvolvementId(String involvementId) {
        this.involvementId = involvementId;
    }

    public Instant getInvolvementEndTime() {
        return involvementEndTime;
    }

    public void setInvolvementEndTime(Instant involvementEndTime) {
        this.involvementEndTime = involvementEndTime;
    }
}
