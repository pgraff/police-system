package com.knowit.policesystem.edge.commands.involvedparties;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.PartyRoleType;
import com.knowit.policesystem.edge.dto.UpdatePartyInvolvementRequestDto;

/**
 * Command for updating a party involvement.
 * Processed by UpdatePartyInvolvementCommandHandler.
 */
public class UpdatePartyInvolvementCommand extends Command {

    private String involvementId;
    private PartyRoleType partyRoleType;
    private String description;

    /**
     * Default constructor for deserialization.
     */
    public UpdatePartyInvolvementCommand() {
        super();
    }

    /**
     * Creates a new update party involvement command from the request DTO.
     *
     * @param aggregateId the aggregate identifier (involvementId from path)
     * @param dto the request DTO containing update data
     */
    public UpdatePartyInvolvementCommand(String aggregateId, UpdatePartyInvolvementRequestDto dto) {
        super(aggregateId);
        this.involvementId = aggregateId;
        this.partyRoleType = dto != null ? dto.getPartyRoleType() : null;
        this.description = dto != null ? dto.getDescription() : null;
    }

    @Override
    public String getCommandType() {
        return "UpdatePartyInvolvementCommand";
    }

    public String getInvolvementId() {
        return involvementId;
    }

    public void setInvolvementId(String involvementId) {
        this.involvementId = involvementId;
    }

    public PartyRoleType getPartyRoleType() {
        return partyRoleType;
    }

    public void setPartyRoleType(PartyRoleType partyRoleType) {
        this.partyRoleType = partyRoleType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
