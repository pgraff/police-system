package com.knowit.policesystem.projection.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.common.events.officershifts.CheckInOfficerRequested;
import com.knowit.policesystem.common.events.officershifts.CheckOutOfficerRequested;
import com.knowit.policesystem.common.events.officershifts.UpdateOfficerShiftRequested;
import com.knowit.policesystem.common.events.shifts.ChangeShiftStatusRequested;
import com.knowit.policesystem.common.events.shifts.EndShiftRequested;
import com.knowit.policesystem.common.events.shifts.RecordShiftChangeRequested;
import com.knowit.policesystem.common.events.shifts.StartShiftRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WorkforceEventParser {

    private static final Logger log = LoggerFactory.getLogger(WorkforceEventParser.class);

    private final ObjectMapper objectMapper;

    public WorkforceEventParser(ObjectMapper objectMapper) {
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
            log.error("Failed to parse workforce event", e);
            throw new IllegalArgumentException("Failed to parse workforce event", e);
        }
    }

    private Object parseByEventType(JsonNode root, String eventType) throws Exception {
        return switch (eventType) {
            // Shift events
            case "StartShiftRequested" -> objectMapper.treeToValue(root, StartShiftRequested.class);
            case "EndShiftRequested" -> objectMapper.treeToValue(root, EndShiftRequested.class);
            case "ChangeShiftStatusRequested" -> objectMapper.treeToValue(root, ChangeShiftStatusRequested.class);
            case "RecordShiftChangeRequested" -> objectMapper.treeToValue(root, RecordShiftChangeRequested.class);
            
            // Officer shift events
            case "CheckInOfficerRequested" -> objectMapper.treeToValue(root, CheckInOfficerRequested.class);
            case "CheckOutOfficerRequested" -> objectMapper.treeToValue(root, CheckOutOfficerRequested.class);
            case "UpdateOfficerShiftRequested" -> objectMapper.treeToValue(root, UpdateOfficerShiftRequested.class);
            
            default -> parseByHeuristics(root, null);
        };
    }

    private Object parseByHeuristics(JsonNode root, String subjectHint) throws Exception {
        // Determine domain from subject hint or field presence
        String domain = determineDomain(root, subjectHint);
        
        return switch (domain) {
            case "shift" -> parseShiftEvent(root, subjectHint);
            case "officer-shift" -> parseOfficerShiftEvent(root, subjectHint);
            default -> throw new IllegalArgumentException("Unknown workforce event domain: " + domain);
        };
    }

    private String determineDomain(JsonNode root, String subjectHint) {
        // Try subject hint first
        if (subjectHint != null) {
            if (subjectHint.contains("shift") && !subjectHint.contains("officer")) return "shift";
            if (subjectHint.contains("officer") && subjectHint.contains("shift")) return "officer-shift";
        }

        // Check for shift-specific fields
        if (hasNonNull(root, "shiftId") && hasNonNull(root, "startTime")) return "shift";
        if (hasNonNull(root, "shiftId") && hasNonNull(root, "endTime")) return "shift";
        if (hasNonNull(root, "shiftId") && hasNonNull(root, "shiftType")) return "shift";
        if (hasNonNull(root, "shiftChangeId")) return "shift";
        
        // Check for officer shift-specific fields
        if (hasNonNull(root, "badgeNumber") && hasNonNull(root, "checkInTime")) return "officer-shift";
        if (hasNonNull(root, "badgeNumber") && hasNonNull(root, "checkOutTime")) return "officer-shift";
        if (hasNonNull(root, "badgeNumber") && hasNonNull(root, "shiftRoleType")) return "officer-shift";

        // Default to shift if shiftId is present
        if (hasNonNull(root, "shiftId")) return "shift";

        throw new IllegalArgumentException("Cannot determine workforce event domain");
    }

    private Object parseShiftEvent(JsonNode root, String subjectHint) throws Exception {
        // StartShiftRequested has startTime, endTime, shiftType, status
        if (hasNonNull(root, "startTime") && hasNonNull(root, "shiftType")) {
            return objectMapper.treeToValue(root, StartShiftRequested.class);
        }
        
        // EndShiftRequested has endTime only
        if (hasNonNull(root, "endTime") && !hasNonNull(root, "startTime")) {
            return objectMapper.treeToValue(root, EndShiftRequested.class);
        }
        
        // ChangeShiftStatusRequested has status only (no time fields)
        if (hasNonNull(root, "status") && !hasNonNull(root, "startTime") && !hasNonNull(root, "endTime") 
                && !hasNonNull(root, "changeTime")) {
            return objectMapper.treeToValue(root, ChangeShiftStatusRequested.class);
        }
        
        // RecordShiftChangeRequested has shiftChangeId, changeTime, changeType
        if (hasNonNull(root, "shiftChangeId") && hasNonNull(root, "changeTime")) {
            return objectMapper.treeToValue(root, RecordShiftChangeRequested.class);
        }

        throw new IllegalArgumentException("Cannot determine shift event type");
    }

    private Object parseOfficerShiftEvent(JsonNode root, String subjectHint) throws Exception {
        // CheckInOfficerRequested has checkInTime and shiftRoleType
        if (hasNonNull(root, "checkInTime") && hasNonNull(root, "shiftRoleType")) {
            return objectMapper.treeToValue(root, CheckInOfficerRequested.class);
        }
        
        // CheckOutOfficerRequested has checkOutTime only
        if (hasNonNull(root, "checkOutTime") && !hasNonNull(root, "checkInTime")) {
            return objectMapper.treeToValue(root, CheckOutOfficerRequested.class);
        }
        
        // UpdateOfficerShiftRequested has shiftRoleType but no time fields
        if (hasNonNull(root, "shiftRoleType") && !hasNonNull(root, "checkInTime") && !hasNonNull(root, "checkOutTime")) {
            return objectMapper.treeToValue(root, UpdateOfficerShiftRequested.class);
        }

        throw new IllegalArgumentException("Cannot determine officer shift event type");
    }

    private boolean hasNonNull(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull();
    }
}
