package com.knowit.policesystem.projection.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.common.events.incidents.ArriveAtIncidentRequested;
import com.knowit.policesystem.common.events.incidents.ChangeIncidentStatusRequested;
import com.knowit.policesystem.common.events.incidents.ClearIncidentRequested;
import com.knowit.policesystem.common.events.incidents.DispatchIncidentRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.common.events.incidents.UpdateIncidentRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IncidentEventParser {

    private static final Logger log = LoggerFactory.getLogger(IncidentEventParser.class);

    private final ObjectMapper objectMapper;

    public IncidentEventParser(ObjectMapper objectMapper) {
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
            log.error("Failed to parse incident event", e);
            throw new IllegalArgumentException("Failed to parse incident event", e);
        }
    }

    private Object parseByEventType(JsonNode root, String eventType) throws Exception {
        return switch (eventType) {
            case "ReportIncidentRequested" -> objectMapper.treeToValue(root, ReportIncidentRequested.class);
            case "UpdateIncidentRequested" -> objectMapper.treeToValue(root, UpdateIncidentRequested.class);
            case "ChangeIncidentStatusRequested" -> objectMapper.treeToValue(root, ChangeIncidentStatusRequested.class);
            case "DispatchIncidentRequested" -> objectMapper.treeToValue(root, DispatchIncidentRequested.class);
            case "ArriveAtIncidentRequested" -> objectMapper.treeToValue(root, ArriveAtIncidentRequested.class);
            case "ClearIncidentRequested" -> objectMapper.treeToValue(root, ClearIncidentRequested.class);
            default -> parseByHeuristics(root, null);
        };
    }

    private Object parseByHeuristics(JsonNode root, String subjectHint) throws Exception {
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

    private boolean hasNonNull(JsonNode root, String field) {
        return root.has(field) && !root.get(field).isNull();
    }

    private boolean subjectIndicatesStatusChange(String subjectHint) {
        return subjectHint != null && subjectHint.contains("status");
    }
}
