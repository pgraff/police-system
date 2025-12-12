package com.knowit.policesystem.projection.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.common.events.dispatches.ChangeDispatchStatusRequested;
import com.knowit.policesystem.common.events.dispatches.CreateDispatchRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DispatchEventParser {

    private static final Logger log = LoggerFactory.getLogger(DispatchEventParser.class);

    private final ObjectMapper objectMapper;

    public DispatchEventParser(ObjectMapper objectMapper) {
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
            log.error("Failed to parse dispatch event", e);
            throw new IllegalArgumentException("Failed to parse dispatch event", e);
        }
    }

    private Object parseByEventType(JsonNode root, String eventType) throws Exception {
        return switch (eventType) {
            case "CreateDispatchRequested" -> objectMapper.treeToValue(root, CreateDispatchRequested.class);
            case "ChangeDispatchStatusRequested" -> objectMapper.treeToValue(root, ChangeDispatchStatusRequested.class);
            default -> parseByHeuristics(root, null);
        };
    }

    private Object parseByHeuristics(JsonNode root, String subjectHint) throws Exception {
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

    private boolean hasNonNull(JsonNode root, String field) {
        return root.has(field) && !root.get(field).isNull();
    }

    private boolean subjectIndicatesStatusChange(String subjectHint) {
        return subjectHint != null && subjectHint.contains("status");
    }
}

