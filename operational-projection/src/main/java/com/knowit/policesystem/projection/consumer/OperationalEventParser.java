package com.knowit.policesystem.projection.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.common.events.activities.ChangeActivityStatusRequested;
import com.knowit.policesystem.common.events.activities.CompleteActivityRequested;
import com.knowit.policesystem.common.events.activities.LinkActivityToIncidentRequested;
import com.knowit.policesystem.common.events.activities.StartActivityRequested;
import com.knowit.policesystem.common.events.activities.UpdateActivityRequested;
import com.knowit.policesystem.common.events.assignments.ChangeAssignmentStatusRequested;
import com.knowit.policesystem.common.events.assignments.CompleteAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.CreateAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.LinkAssignmentToDispatchRequested;
import com.knowit.policesystem.common.events.calls.ArriveAtCallRequested;
import com.knowit.policesystem.common.events.calls.ChangeCallStatusRequested;
import com.knowit.policesystem.common.events.calls.ClearCallRequested;
import com.knowit.policesystem.common.events.calls.DispatchCallRequested;
import com.knowit.policesystem.common.events.calls.LinkCallToDispatchRequested;
import com.knowit.policesystem.common.events.calls.LinkCallToIncidentRequested;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.common.events.calls.UpdateCallRequested;
import com.knowit.policesystem.common.events.dispatches.ChangeDispatchStatusRequested;
import com.knowit.policesystem.common.events.dispatches.CreateDispatchRequested;
import com.knowit.policesystem.common.events.incidents.ArriveAtIncidentRequested;
import com.knowit.policesystem.common.events.incidents.ChangeIncidentStatusRequested;
import com.knowit.policesystem.common.events.incidents.ClearIncidentRequested;
import com.knowit.policesystem.common.events.incidents.DispatchIncidentRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.common.events.incidents.UpdateIncidentRequested;
import com.knowit.policesystem.common.events.involvedparty.EndPartyInvolvementRequested;
import com.knowit.policesystem.common.events.involvedparty.InvolvePartyRequested;
import com.knowit.policesystem.common.events.involvedparty.UpdatePartyInvolvementRequested;
import com.knowit.policesystem.common.events.resourceassignment.AssignResourceRequested;
import com.knowit.policesystem.common.events.resourceassignment.ChangeResourceAssignmentStatusRequested;
import com.knowit.policesystem.common.events.resourceassignment.UnassignResourceRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OperationalEventParser {

    private static final Logger log = LoggerFactory.getLogger(OperationalEventParser.class);

    private final ObjectMapper objectMapper;

    public OperationalEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Object parse(String payload, String subjectHint) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.has("eventType") ? root.get("eventType").asText() : null;

            // Use eventType if available (most reliable)
            if (eventType != null) {
                return parseByEventType(root, eventType);
            }

            // Fallback to heuristics based on field presence
            return parseByHeuristics(root, subjectHint);
        } catch (Exception e) {
            log.error("Failed to parse operational event", e);
            throw new IllegalArgumentException("Failed to parse operational event", e);
        }
    }

    private Object parseByEventType(JsonNode root, String eventType) throws Exception {
        return switch (eventType) {
            // Incident events
            case "ReportIncidentRequested" -> objectMapper.treeToValue(root, ReportIncidentRequested.class);
            case "UpdateIncidentRequested" -> objectMapper.treeToValue(root, UpdateIncidentRequested.class);
            case "ChangeIncidentStatusRequested" -> objectMapper.treeToValue(root, ChangeIncidentStatusRequested.class);
            case "DispatchIncidentRequested" -> objectMapper.treeToValue(root, DispatchIncidentRequested.class);
            case "ArriveAtIncidentRequested" -> objectMapper.treeToValue(root, ArriveAtIncidentRequested.class);
            case "ClearIncidentRequested" -> objectMapper.treeToValue(root, ClearIncidentRequested.class);
            
            // Call events
            case "ReceiveCallRequested" -> objectMapper.treeToValue(root, ReceiveCallRequested.class);
            case "UpdateCallRequested" -> objectMapper.treeToValue(root, UpdateCallRequested.class);
            case "ChangeCallStatusRequested" -> objectMapper.treeToValue(root, ChangeCallStatusRequested.class);
            case "DispatchCallRequested" -> objectMapper.treeToValue(root, DispatchCallRequested.class);
            case "ArriveAtCallRequested" -> objectMapper.treeToValue(root, ArriveAtCallRequested.class);
            case "ClearCallRequested" -> objectMapper.treeToValue(root, ClearCallRequested.class);
            case "LinkCallToIncidentRequested" -> objectMapper.treeToValue(root, LinkCallToIncidentRequested.class);
            case "LinkCallToDispatchRequested" -> objectMapper.treeToValue(root, LinkCallToDispatchRequested.class);
            
            // Dispatch events
            case "CreateDispatchRequested" -> objectMapper.treeToValue(root, CreateDispatchRequested.class);
            case "ChangeDispatchStatusRequested" -> objectMapper.treeToValue(root, ChangeDispatchStatusRequested.class);
            
            // Activity events
            case "StartActivityRequested" -> objectMapper.treeToValue(root, StartActivityRequested.class);
            case "UpdateActivityRequested" -> objectMapper.treeToValue(root, UpdateActivityRequested.class);
            case "ChangeActivityStatusRequested" -> objectMapper.treeToValue(root, ChangeActivityStatusRequested.class);
            case "CompleteActivityRequested" -> objectMapper.treeToValue(root, CompleteActivityRequested.class);
            case "LinkActivityToIncidentRequested" -> objectMapper.treeToValue(root, LinkActivityToIncidentRequested.class);
            
            // Assignment events
            case "CreateAssignmentRequested" -> objectMapper.treeToValue(root, CreateAssignmentRequested.class);
            case "ChangeAssignmentStatusRequested" -> objectMapper.treeToValue(root, ChangeAssignmentStatusRequested.class);
            case "CompleteAssignmentRequested" -> objectMapper.treeToValue(root, CompleteAssignmentRequested.class);
            case "LinkAssignmentToDispatchRequested" -> objectMapper.treeToValue(root, LinkAssignmentToDispatchRequested.class);
            
            // Involved party events
            case "InvolvePartyRequested" -> objectMapper.treeToValue(root, InvolvePartyRequested.class);
            case "UpdatePartyInvolvementRequested" -> objectMapper.treeToValue(root, UpdatePartyInvolvementRequested.class);
            case "EndPartyInvolvementRequested" -> objectMapper.treeToValue(root, EndPartyInvolvementRequested.class);
            
            // Resource assignment events
            case "AssignResourceRequested" -> objectMapper.treeToValue(root, AssignResourceRequested.class);
            case "UnassignResourceRequested" -> objectMapper.treeToValue(root, UnassignResourceRequested.class);
            case "ChangeResourceAssignmentStatusRequested" -> objectMapper.treeToValue(root, ChangeResourceAssignmentStatusRequested.class);
            
            default -> parseByHeuristics(root, null);
        };
    }

    private Object parseByHeuristics(JsonNode root, String subjectHint) throws Exception {
        // Determine domain from subject hint or field presence
        String domain = determineDomain(root, subjectHint);
        
        return switch (domain) {
            case "incident" -> parseIncidentEvent(root, subjectHint);
            case "call" -> parseCallEvent(root, subjectHint);
            case "dispatch" -> parseDispatchEvent(root, subjectHint);
            case "activity" -> parseActivityEvent(root, subjectHint);
            case "assignment" -> parseAssignmentEvent(root, subjectHint);
            case "involved-party" -> parseInvolvedPartyEvent(root, subjectHint);
            case "resource-assignment" -> parseResourceAssignmentEvent(root, subjectHint);
            default -> throw new IllegalArgumentException("Unknown operational event domain: " + domain);
        };
    }

    private String determineDomain(JsonNode root, String subjectHint) {
        // Try subject hint first
        if (subjectHint != null) {
            if (subjectHint.contains("incident")) return "incident";
            if (subjectHint.contains("call")) return "call";
            if (subjectHint.contains("dispatch")) return "dispatch";
            if (subjectHint.contains("activity")) return "activity";
            if (subjectHint.contains("assignment") && !subjectHint.contains("resource")) return "assignment";
            if (subjectHint.contains("involved-party") || subjectHint.contains("involvedparty")) return "involved-party";
            if (subjectHint.contains("resource-assignment") || subjectHint.contains("resourceassignment")) return "resource-assignment";
        }
        
        // Fallback to field presence - check more specific fields first to avoid false positives
        // Resource assignment has resourceId/resourceType - check this first as it's most specific
        if (hasNonNull(root, "resourceId") || hasNonNull(root, "resourceType")) {
            return "resource-assignment";
        }
        // Involved party has involvementId - check this early
        if (hasNonNull(root, "involvementId") || hasNonNull(root, "partyRoleType")) {
            return "involved-party";
        }
        // Assignment has assignedTime/assignmentType - check before incident (which might have incidentId)
        if (hasNonNull(root, "assignedTime") || hasNonNull(root, "assignmentType")) {
            return "assignment";
        }
        // Activity has activityTime/activityType - check before incident
        if (hasNonNull(root, "activityTime") || hasNonNull(root, "activityType")) {
            return "activity";
        }
        // Dispatch has dispatchTime/dispatchType
        if (hasNonNull(root, "dispatchTime") || hasNonNull(root, "dispatchType")) {
            return "dispatch";
        }
        // Call has callNumber/receivedTime
        if (hasNonNull(root, "callNumber") || hasNonNull(root, "receivedTime")) {
            return "call";
        }
        // Incident has incidentNumber/reportedTime
        if (hasNonNull(root, "incidentNumber") || hasNonNull(root, "reportedTime")) {
            return "incident";
        }
        // Fallback to ID fields (less specific, so check last)
        if (hasNonNull(root, "assignmentId")) {
            return "assignment";
        }
        if (hasNonNull(root, "activityId")) {
            return "activity";
        }
        if (hasNonNull(root, "dispatchId")) {
            return "dispatch";
        }
        if (hasNonNull(root, "callId")) {
            return "call";
        }
        if (hasNonNull(root, "incidentId")) {
            return "incident";
        }
        
        throw new IllegalArgumentException("Cannot determine domain for operational event");
    }

    private Object parseIncidentEvent(JsonNode root, String subjectHint) throws Exception {
        // Check for timestamp fields to identify action events
        boolean hasDispatchedTime = hasNonNull(root, "dispatchedTime");
        boolean hasArrivedTime = hasNonNull(root, "arrivedTime");
        boolean hasClearedTime = hasNonNull(root, "clearedTime");
        boolean hasReportedTime = hasNonNull(root, "reportedTime");

        // Action events (single timestamp field)
        if (hasDispatchedTime && !hasArrivedTime && !hasClearedTime) {
            return objectMapper.treeToValue(root, DispatchIncidentRequested.class);
        }
        if (hasArrivedTime && !hasDispatchedTime && !hasClearedTime) {
            return objectMapper.treeToValue(root, ArriveAtIncidentRequested.class);
        }
        if (hasClearedTime && !hasDispatchedTime && !hasArrivedTime) {
            return objectMapper.treeToValue(root, ClearIncidentRequested.class);
        }

        // Check for status change
        boolean hasStatus = hasNonNull(root, "status");
        boolean hasIncidentFields = hasNonNull(root, "incidentNumber")
                || hasNonNull(root, "priority")
                || hasNonNull(root, "description")
                || hasNonNull(root, "incidentType");

        // Status change (only status field, or subject hint)
        if (hasStatus && !hasIncidentFields && !hasReportedTime) {
            return objectMapper.treeToValue(root, ChangeIncidentStatusRequested.class);
        }
        if (subjectIndicatesStatusChange(subjectHint)) {
            return objectMapper.treeToValue(root, ChangeIncidentStatusRequested.class);
        }

        // Report vs Update: Report has reportedTime and incidentNumber
        if (hasReportedTime && hasNonNull(root, "incidentNumber")) {
            return objectMapper.treeToValue(root, ReportIncidentRequested.class);
        }

        // Update (has incident fields but no reportedTime)
        if (hasIncidentFields) {
            return objectMapper.treeToValue(root, UpdateIncidentRequested.class);
        }

        throw new IllegalArgumentException("Unknown incident event payload");
    }

    private Object parseCallEvent(JsonNode root, String subjectHint) throws Exception {
        // Check for timestamp fields to identify action events
        boolean hasDispatchedTime = hasNonNull(root, "dispatchedTime");
        boolean hasArrivedTime = hasNonNull(root, "arrivedTime");
        boolean hasClearedTime = hasNonNull(root, "clearedTime");
        boolean hasReceivedTime = hasNonNull(root, "receivedTime");

        // Link events (have target IDs)
        if (hasNonNull(root, "incidentId") && !hasReceivedTime && !hasDispatchedTime) {
            return objectMapper.treeToValue(root, LinkCallToIncidentRequested.class);
        }
        if (hasNonNull(root, "dispatchId") && !hasReceivedTime && !hasDispatchedTime) {
            return objectMapper.treeToValue(root, LinkCallToDispatchRequested.class);
        }

        // Action events (single timestamp field)
        if (hasDispatchedTime && !hasArrivedTime && !hasClearedTime) {
            return objectMapper.treeToValue(root, DispatchCallRequested.class);
        }
        if (hasArrivedTime && !hasDispatchedTime && !hasClearedTime) {
            return objectMapper.treeToValue(root, ArriveAtCallRequested.class);
        }
        if (hasClearedTime && !hasDispatchedTime && !hasArrivedTime) {
            return objectMapper.treeToValue(root, ClearCallRequested.class);
        }

        // Check for status change
        boolean hasStatus = hasNonNull(root, "status");
        boolean hasCallFields = hasNonNull(root, "callNumber")
                || hasNonNull(root, "priority")
                || hasNonNull(root, "description")
                || hasNonNull(root, "callType");

        // Status change (only status field, or subject hint)
        if (hasStatus && !hasCallFields && !hasReceivedTime) {
            return objectMapper.treeToValue(root, ChangeCallStatusRequested.class);
        }
        if (subjectIndicatesStatusChange(subjectHint)) {
            return objectMapper.treeToValue(root, ChangeCallStatusRequested.class);
        }

        // Receive vs Update: Receive has receivedTime and callNumber
        if (hasReceivedTime && hasNonNull(root, "callNumber")) {
            return objectMapper.treeToValue(root, ReceiveCallRequested.class);
        }

        // Update (has call fields but no receivedTime)
        if (hasCallFields) {
            return objectMapper.treeToValue(root, UpdateCallRequested.class);
        }

        throw new IllegalArgumentException("Unknown call event payload");
    }

    private Object parseDispatchEvent(JsonNode root, String subjectHint) throws Exception {
        // Check for dispatch creation fields
        boolean hasDispatchTime = hasNonNull(root, "dispatchTime");
        boolean hasDispatchType = hasNonNull(root, "dispatchType");
        boolean hasStatus = hasNonNull(root, "status");

        // CreateDispatchRequested has dispatchTime, dispatchType, and status
        if (hasDispatchTime && hasDispatchType && hasStatus) {
            return objectMapper.treeToValue(root, CreateDispatchRequested.class);
        }

        // ChangeDispatchStatusRequested has only status (and dispatchId)
        if (hasStatus && !hasDispatchTime && !hasDispatchType) {
            return objectMapper.treeToValue(root, ChangeDispatchStatusRequested.class);
        }

        // Subject hint fallback
        if (subjectIndicatesStatusChange(subjectHint)) {
            return objectMapper.treeToValue(root, ChangeDispatchStatusRequested.class);
        }

        throw new IllegalArgumentException("Unknown dispatch event payload");
    }

    private Object parseActivityEvent(JsonNode root, String subjectHint) throws Exception {
        // Check for link event
        if (hasNonNull(root, "incidentId") && !hasNonNull(root, "activityTime") && !hasNonNull(root, "activityType")) {
            return objectMapper.treeToValue(root, LinkActivityToIncidentRequested.class);
        }

        // Check for timestamp fields to identify action events
        boolean hasCompletedTime = hasNonNull(root, "completedTime");
        boolean hasActivityTime = hasNonNull(root, "activityTime");
        boolean hasStatus = hasNonNull(root, "status");
        boolean hasActivityFields = hasNonNull(root, "activityType")
                || hasNonNull(root, "description");

        // CompleteActivityRequested has only completedTime
        if (hasCompletedTime && !hasActivityTime && !hasActivityFields) {
            return objectMapper.treeToValue(root, CompleteActivityRequested.class);
        }

        // StartActivityRequested has activityTime, activityType, description, status
        if (hasActivityTime && hasActivityFields && hasStatus) {
            return objectMapper.treeToValue(root, StartActivityRequested.class);
        }

        // ChangeActivityStatusRequested has only status (and activityId)
        if (hasStatus && !hasActivityTime && !hasActivityFields && !hasCompletedTime) {
            return objectMapper.treeToValue(root, ChangeActivityStatusRequested.class);
        }
        if (subjectIndicatesStatusChange(subjectHint)) {
            return objectMapper.treeToValue(root, ChangeActivityStatusRequested.class);
        }

        // UpdateActivityRequested has description (and activityId)
        if (hasActivityFields && !hasActivityTime && !hasStatus && !hasCompletedTime) {
            return objectMapper.treeToValue(root, UpdateActivityRequested.class);
        }

        throw new IllegalArgumentException("Unknown activity event payload");
    }

    private Object parseAssignmentEvent(JsonNode root, String subjectHint) throws Exception {
        boolean hasAssignedTime = hasNonNull(root, "assignedTime");
        boolean hasAssignmentType = hasNonNull(root, "assignmentType");
        boolean hasStatus = hasNonNull(root, "status");
        boolean hasCompletedTime = hasNonNull(root, "completedTime");
        boolean hasDispatchId = hasNonNull(root, "dispatchId");
        boolean hasResourceId = hasNonNull(root, "resourceId");
        boolean hasResourceType = hasNonNull(root, "resourceType");

        // Resource assignment events are in resource-assignment topic, but check here for safety
        if (hasResourceId || hasResourceType) {
            // This should not happen in assignment topic, but handle gracefully
            throw new IllegalArgumentException("Resource assignment event found in assignment topic");
        }

        // LinkAssignmentToDispatchRequested has only dispatchId
        if (hasDispatchId && !hasAssignedTime && !hasAssignmentType) {
            return objectMapper.treeToValue(root, LinkAssignmentToDispatchRequested.class);
        }

        // CompleteAssignmentRequested has completedTime
        if (hasCompletedTime && !hasAssignedTime) {
            return objectMapper.treeToValue(root, CompleteAssignmentRequested.class);
        }

        // CreateAssignmentRequested has assignedTime, assignmentType, and status
        if (hasAssignedTime && hasAssignmentType && hasStatus) {
            return objectMapper.treeToValue(root, CreateAssignmentRequested.class);
        }

        // ChangeAssignmentStatusRequested has only status
        if (hasStatus) {
            return objectMapper.treeToValue(root, ChangeAssignmentStatusRequested.class);
        }

        // Subject hint fallback
        if (subjectIndicatesStatusChange(subjectHint)) {
            return objectMapper.treeToValue(root, ChangeAssignmentStatusRequested.class);
        }

        throw new IllegalArgumentException("Unknown assignment event payload");
    }

    private Object parseInvolvedPartyEvent(JsonNode root, String subjectHint) throws Exception {
        boolean hasInvolvementStartTime = hasNonNull(root, "involvementStartTime");
        boolean hasInvolvementEndTime = hasNonNull(root, "involvementEndTime");
        boolean hasPersonId = hasNonNull(root, "personId");
        boolean hasPartyRoleType = hasNonNull(root, "partyRoleType");
        boolean hasDescription = hasNonNull(root, "description");

        // EndPartyInvolvementRequested has involvementEndTime
        if (hasInvolvementEndTime && !hasInvolvementStartTime) {
            return objectMapper.treeToValue(root, EndPartyInvolvementRequested.class);
        }

        // InvolvePartyRequested has involvementStartTime, personId, partyRoleType
        if (hasInvolvementStartTime && hasPersonId && hasPartyRoleType) {
            return objectMapper.treeToValue(root, InvolvePartyRequested.class);
        }

        // UpdatePartyInvolvementRequested has description or other fields but no start/end times
        if (hasDescription || (hasPartyRoleType && !hasInvolvementStartTime && !hasInvolvementEndTime)) {
            return objectMapper.treeToValue(root, UpdatePartyInvolvementRequested.class);
        }

        throw new IllegalArgumentException("Unknown involved party event payload");
    }

    private Object parseResourceAssignmentEvent(JsonNode root, String subjectHint) throws Exception {
        boolean hasResourceId = hasNonNull(root, "resourceId");
        boolean hasResourceType = hasNonNull(root, "resourceType");
        boolean hasStatus = hasNonNull(root, "status");
        boolean hasStartTime = hasNonNull(root, "startTime");
        boolean hasEndTime = hasNonNull(root, "endTime");
        boolean hasAssignmentId = hasNonNull(root, "assignmentId");

        // UnassignResourceRequested has endTime
        if (hasEndTime && !hasStartTime) {
            return objectMapper.treeToValue(root, UnassignResourceRequested.class);
        }

        // AssignResourceRequested has startTime, resourceId, resourceType
        if (hasStartTime && hasResourceId && hasResourceType && hasAssignmentId) {
            return objectMapper.treeToValue(root, AssignResourceRequested.class);
        }

        // ChangeResourceAssignmentStatusRequested has only status
        if (hasStatus && !hasStartTime && !hasEndTime) {
            return objectMapper.treeToValue(root, ChangeResourceAssignmentStatusRequested.class);
        }
        if (subjectIndicatesStatusChange(subjectHint)) {
            return objectMapper.treeToValue(root, ChangeResourceAssignmentStatusRequested.class);
        }

        throw new IllegalArgumentException("Unknown resource assignment event payload");
    }

    private boolean hasNonNull(JsonNode root, String field) {
        return root.has(field) && !root.get(field).isNull();
    }

    private boolean subjectIndicatesStatusChange(String subjectHint) {
        return subjectHint != null && subjectHint.contains("status");
    }
}
