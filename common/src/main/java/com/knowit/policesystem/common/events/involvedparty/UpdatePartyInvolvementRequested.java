package com.knowit.policesystem.common.events.involvedparty;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to update a party involvement.
 * Published to Kafka when a party involvement update is requested via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class UpdatePartyInvolvementRequested extends Event {

    private String involvementId;
    private String partyRoleType;
    private String description;

    /**
     * Default constructor for deserialization.
     */
    public UpdatePartyInvolvementRequested() {
        super();
    }

    /**
     * Creates a new UpdatePartyInvolvementRequested event.
     *
     * @param aggregateId the aggregate identifier (involvementId)
     * @param involvementId the involvement identifier
     * @param partyRoleType the party role type (optional, as string)
     * @param description the description (optional)
     */
    public UpdatePartyInvolvementRequested(String aggregateId, String involvementId, String partyRoleType, String description) {
        super(aggregateId);
        this.involvementId = involvementId;
        this.partyRoleType = partyRoleType;
        this.description = description;
    }

    @Override
    public String getEventType() {
        return "UpdatePartyInvolvementRequested";
    }

    public String getInvolvementId() {
        return involvementId;
    }

    public void setInvolvementId(String involvementId) {
        this.involvementId = involvementId;
    }

    public String getPartyRoleType() {
        return partyRoleType;
    }

    public void setPartyRoleType(String partyRoleType) {
        this.partyRoleType = partyRoleType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
