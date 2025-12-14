package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.knowit.policesystem.edge.domain.PartyRoleType;
import com.knowit.policesystem.edge.util.FlexibleInstantDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for involving a party in an incident, call, or activity.
 * Matches the InvolvePartyRequest schema in the OpenAPI specification.
 */
public class InvolvePartyRequestDto {

    @NotBlank(message = "personId is required")
    private String personId;

    private String incidentId;

    private String callId;

    private String activityId;

    @NotNull(message = "partyRoleType is required")
    private PartyRoleType partyRoleType;

    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    @JsonDeserialize(using = FlexibleInstantDeserializer.class)
    private Instant involvementStartTime;

    /**
     * Default constructor for Jackson deserialization.
     */
    public InvolvePartyRequestDto() {
    }

    /**
     * Creates a new involve party request DTO.
     *
     * @param personId the person identifier
     * @param incidentId the incident identifier (optional, mutually exclusive with callId and activityId)
     * @param callId the call identifier (optional, mutually exclusive with incidentId and activityId)
     * @param activityId the activity identifier (optional, mutually exclusive with incidentId and callId)
     * @param partyRoleType the party role type
     * @param description the description (optional)
     * @param involvementStartTime the involvement start time (optional)
     */
    public InvolvePartyRequestDto(String personId, String incidentId, String callId, String activityId,
                                 PartyRoleType partyRoleType, String description, Instant involvementStartTime) {
        this.personId = personId;
        this.incidentId = incidentId;
        this.callId = callId;
        this.activityId = activityId;
        this.partyRoleType = partyRoleType;
        this.description = description;
        this.involvementStartTime = involvementStartTime;
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
