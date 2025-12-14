package com.knowit.policesystem.edge.dto;

import com.knowit.policesystem.edge.domain.AssignmentStatus;
import com.knowit.policesystem.edge.domain.AssignmentType;
import com.knowit.policesystem.edge.domain.IncidentStatus;
import com.knowit.policesystem.edge.domain.IncidentType;
import com.knowit.policesystem.edge.domain.Priority;
import com.knowit.policesystem.edge.domain.ResourceType;
import com.knowit.policesystem.edge.domain.RoleType;
import com.knowit.policesystem.edge.util.FlexibleInstantDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * Request DTO for incident dispatch workflow.
 * Orchestrates creating an incident, dispatching it, creating an assignment, and assigning resources in one call.
 */
public class IncidentDispatchWorkflowRequestDto {

    @Valid
    @NotNull(message = "incident is required")
    private IncidentData incident;

    @Valid
    private DispatchData dispatch;

    @Valid
    private AssignmentData assignment;

    @Valid
    private List<ResourceData> resources;

    private String locationId;

    private List<String> callIds;

    public static class IncidentData {
        @NotBlank(message = "incidentId is required")
        private String incidentId;

        private String incidentNumber;

        @NotNull(message = "priority is required")
        private Priority priority;

        @NotNull(message = "status is required")
        private IncidentStatus status;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
        @JsonDeserialize(using = FlexibleInstantDeserializer.class)
        private Instant reportedTime;

        private String description;

        @NotNull(message = "incidentType is required")
        private IncidentType incidentType;

        // Getters and setters
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
        public String getIncidentNumber() { return incidentNumber; }
        public void setIncidentNumber(String incidentNumber) { this.incidentNumber = incidentNumber; }
        public Priority getPriority() { return priority; }
        public void setPriority(Priority priority) { this.priority = priority; }
        public IncidentStatus getStatus() { return status; }
        public void setStatus(IncidentStatus status) { this.status = status; }
        public Instant getReportedTime() { return reportedTime; }
        public void setReportedTime(Instant reportedTime) { this.reportedTime = reportedTime; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public IncidentType getIncidentType() { return incidentType; }
        public void setIncidentType(IncidentType incidentType) { this.incidentType = incidentType; }
    }

    public static class DispatchData {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
        @JsonDeserialize(using = FlexibleInstantDeserializer.class)
        private Instant dispatchedTime;

        private String unitId;

        public Instant getDispatchedTime() { return dispatchedTime; }
        public void setDispatchedTime(Instant dispatchedTime) { this.dispatchedTime = dispatchedTime; }
        public String getUnitId() { return unitId; }
        public void setUnitId(String unitId) { this.unitId = unitId; }
    }

    public static class AssignmentData {
        @NotBlank(message = "assignmentId is required")
        private String assignmentId;

        @NotBlank(message = "badgeNumber is required")
        private String badgeNumber;

        @NotNull(message = "assignmentType is required")
        private AssignmentType assignmentType;

        @NotNull(message = "status is required")
        private AssignmentStatus status;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
        @JsonDeserialize(using = FlexibleInstantDeserializer.class)
        private Instant assignedTime;

        public String getAssignmentId() { return assignmentId; }
        public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }
        public String getBadgeNumber() { return badgeNumber; }
        public void setBadgeNumber(String badgeNumber) { this.badgeNumber = badgeNumber; }
        public AssignmentType getAssignmentType() { return assignmentType; }
        public void setAssignmentType(AssignmentType assignmentType) { this.assignmentType = assignmentType; }
        public AssignmentStatus getStatus() { return status; }
        public void setStatus(AssignmentStatus status) { this.status = status; }
        public Instant getAssignedTime() { return assignedTime; }
        public void setAssignedTime(Instant assignedTime) { this.assignedTime = assignedTime; }
    }

    public static class ResourceData {
        @NotBlank(message = "resourceId is required")
        private String resourceId;

        @NotNull(message = "resourceType is required")
        private ResourceType resourceType;

        @NotNull(message = "roleType is required")
        private RoleType roleType;

        @NotBlank(message = "status is required")
        private String status;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
        @JsonDeserialize(using = FlexibleInstantDeserializer.class)
        private Instant startTime;

        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }
        public ResourceType getResourceType() { return resourceType; }
        public void setResourceType(ResourceType resourceType) { this.resourceType = resourceType; }
        public RoleType getRoleType() { return roleType; }
        public void setRoleType(RoleType roleType) { this.roleType = roleType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Instant getStartTime() { return startTime; }
        public void setStartTime(Instant startTime) { this.startTime = startTime; }
    }

    // Main DTO getters and setters
    public IncidentData getIncident() { return incident; }
    public void setIncident(IncidentData incident) { this.incident = incident; }
    public DispatchData getDispatch() { return dispatch; }
    public void setDispatch(DispatchData dispatch) { this.dispatch = dispatch; }
    public AssignmentData getAssignment() { return assignment; }
    public void setAssignment(AssignmentData assignment) { this.assignment = assignment; }
    public List<ResourceData> getResources() { return resources; }
    public void setResources(List<ResourceData> resources) { this.resources = resources; }
    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }
    public List<String> getCallIds() { return callIds; }
    public void setCallIds(List<String> callIds) { this.callIds = callIds; }
}
