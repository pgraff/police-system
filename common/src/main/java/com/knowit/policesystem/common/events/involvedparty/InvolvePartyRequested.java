package com.knowit.policesystem.common.events.involvedparty;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to involve a party in an incident, call, or activity.
 * This event is published to Kafka when a party is involved via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class InvolvePartyRequested extends Event {

    private String involvementId;
    private String personId;
    private String incidentId;
    private String callId;
    private String activityId;
    private String partyRoleType;
    private String description;
    private Instant involvementStartTime;

    /**
     * Default constructor for deserialization.
     */
    public InvolvePartyRequested() {
        super();
    }

    /**
     * Creates a new InvolvePartyRequested event.
     *
     * @param aggregateId the aggregate identifier (involvementId)
     * @param involvementId the involvement identifier
     * @param personId the person identifier
     * @param incidentId the incident identifier (optional, mutually exclusive with callId and activityId)
     * @param callId the call identifier (optional, mutually exclusive with incidentId and activityId)
     * @param activityId the activity identifier (optional, mutually exclusive with incidentId and callId)
     * @param partyRoleType the party role type as string
     * @param description the description (optional)
     * @param involvementStartTime the involvement start time (optional)
     */
    public InvolvePartyRequested(String aggregateId, String involvementId, String personId,
                                 String incidentId, String callId, String activityId,
                                 String partyRoleType, String description, Instant involvementStartTime) {
        super(aggregateId);
        this.involvementId = involvementId;
        this.personId = personId;
        this.incidentId = incidentId;
        this.callId = callId;
        this.activityId = activityId;
        this.partyRoleType = partyRoleType;
        this.description = description;
        this.involvementStartTime = involvementStartTime;
    }

    @Override
    public String getEventType() {
        return "InvolvePartyRequested";
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

    public Instant getInvolvementStartTime() {
        return involvementStartTime;
    }

    public void setInvolvementStartTime(Instant involvementStartTime) {
        this.involvementStartTime = involvementStartTime;
    }
}
