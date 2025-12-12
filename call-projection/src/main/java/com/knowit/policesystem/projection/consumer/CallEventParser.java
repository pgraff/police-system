package com.knowit.policesystem.projection.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.common.events.calls.ArriveAtCallRequested;
import com.knowit.policesystem.common.events.calls.ChangeCallStatusRequested;
import com.knowit.policesystem.common.events.calls.ClearCallRequested;
import com.knowit.policesystem.common.events.calls.DispatchCallRequested;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.common.events.calls.UpdateCallRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CallEventParser {

    private static final Logger log = LoggerFactory.getLogger(CallEventParser.class);

    private final ObjectMapper objectMapper;

    public CallEventParser(ObjectMapper objectMapper) {
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
            log.error("Failed to parse call event", e);
            throw new IllegalArgumentException("Failed to parse call event", e);
        }
    }

    private Object parseByEventType(JsonNode root, String eventType) throws Exception {
        return switch (eventType) {
            case "ReceiveCallRequested" -> objectMapper.treeToValue(root, ReceiveCallRequested.class);
            case "UpdateCallRequested" -> objectMapper.treeToValue(root, UpdateCallRequested.class);
            case "ChangeCallStatusRequested" -> objectMapper.treeToValue(root, ChangeCallStatusRequested.class);
            case "DispatchCallRequested" -> objectMapper.treeToValue(root, DispatchCallRequested.class);
            case "ArriveAtCallRequested" -> objectMapper.treeToValue(root, ArriveAtCallRequested.class);
            case "ClearCallRequested" -> objectMapper.treeToValue(root, ClearCallRequested.class);
            default -> parseByHeuristics(root, null);
        };
    }

    private Object parseByHeuristics(JsonNode root, String subjectHint) throws Exception {
        // Check for timestamp fields to identify action events
        boolean hasDispatchedTime = hasNonNull(root, "dispatchedTime");
        boolean hasArrivedTime = hasNonNull(root, "arrivedTime");
        boolean hasClearedTime = hasNonNull(root, "clearedTime");
        boolean hasReceivedTime = hasNonNull(root, "receivedTime");

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

    private boolean hasNonNull(JsonNode root, String field) {
        return root.has(field) && !root.get(field).isNull();
    }

    private boolean subjectIndicatesStatusChange(String subjectHint) {
        return subjectHint != null && subjectHint.contains("status");
    }
}

