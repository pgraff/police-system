package com.knowit.policesystem.edge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * DTO for related resource IDs in responses.
 * Helps UI clients by providing IDs of related resources without requiring separate queries.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RelatedResourcesDto {

    private String dispatchId;
    private List<String> assignmentIds;
    private List<String> callIds;
    private String locationId;
    private List<String> activityIds;
    private List<String> involvedPartyIds;
    private String unitId;
    private String vehicleId;

    /**
     * Default constructor for Jackson serialization.
     */
    public RelatedResourcesDto() {
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }

    public List<String> getAssignmentIds() {
        return assignmentIds;
    }

    public void setAssignmentIds(List<String> assignmentIds) {
        this.assignmentIds = assignmentIds;
    }

    public List<String> getCallIds() {
        return callIds;
    }

    public void setCallIds(List<String> callIds) {
        this.callIds = callIds;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public List<String> getActivityIds() {
        return activityIds;
    }

    public void setActivityIds(List<String> activityIds) {
        this.activityIds = activityIds;
    }

    public List<String> getInvolvedPartyIds() {
        return involvedPartyIds;
    }

    public void setInvolvedPartyIds(List<String> involvedPartyIds) {
        this.involvedPartyIds = involvedPartyIds;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }
}
