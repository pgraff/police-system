package com.knowit.policesystem.edge.commands.involvedparties;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.PartyRoleType;
import com.knowit.policesystem.edge.dto.InvolvePartyRequestDto;

import java.time.Instant;

/**
 * Command for involving a party in an incident, call, or activity.
 * This command is processed by InvolvePartyCommandHandler.
 */
public class InvolvePartyCommand extends Command {

    private String involvementId;
    private String personId;
    private String incidentId;
    private String callId;
    private String activityId;
    private PartyRoleType partyRoleType;
    private String description;
    private Instant involvementStartTime;

    /**
     * Default constructor for deserialization.
     */
    public InvolvePartyCommand() {
        super();
    }

    /**
     * Creates a new involve party command from a DTO.
     *
     * @param aggregateId the aggregate identifier (involvementId)
     * @param dto the request DTO containing involvement data
     */
    public InvolvePartyCommand(String aggregateId, InvolvePartyRequestDto dto) {
        super(aggregateId);
        this.involvementId = aggregateId;
        this.personId = dto.getPersonId();
        this.incidentId = dto.getIncidentId();
        this.callId = dto.getCallId();
        this.activityId = dto.getActivityId();
        this.partyRoleType = dto.getPartyRoleType();
        this.description = dto.getDescription();
        this.involvementStartTime = dto.getInvolvementStartTime();
    }

    @Override
    public String getCommandType() {
        return "InvolvePartyCommand";
    }

    public String getInvolvementId() {
        return involvementId;
    }

    public void setInvolvementId(String involvementId) {
        this.involvementId = involvementId;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
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

    public Instant getInvolvementStartTime() {
        return involvementStartTime;
    }

    public void setInvolvementStartTime(Instant involvementStartTime) {
        this.involvementStartTime = involvementStartTime;
    }
}
