package com.knowit.policesystem.projection.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.common.events.activities.ChangeActivityStatusRequested;
import com.knowit.policesystem.common.events.activities.CompleteActivityRequested;
import com.knowit.policesystem.common.events.activities.StartActivityRequested;
import com.knowit.policesystem.common.events.activities.UpdateActivityRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ActivityEventParser {

    private static final Logger log = LoggerFactory.getLogger(ActivityEventParser.class);

    private final ObjectMapper objectMapper;

    public ActivityEventParser(ObjectMapper objectMapper) {
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
            log.error("Failed to parse activity event", e);
            throw new IllegalArgumentException("Failed to parse activity event", e);
        }
    }

    private Object parseByEventType(JsonNode root, String eventType) throws Exception {
        return switch (eventType) {
            case "StartActivityRequested" -> objectMapper.treeToValue(root, StartActivityRequested.class);
            case "UpdateActivityRequested" -> objectMapper.treeToValue(root, UpdateActivityRequested.class);
            case "ChangeActivityStatusRequested" -> objectMapper.treeToValue(root, ChangeActivityStatusRequested.class);
            case "CompleteActivityRequested" -> objectMapper.treeToValue(root, CompleteActivityRequested.class);
            default -> parseByHeuristics(root, null);
        };
    }

    private Object parseByHeuristics(JsonNode root, String subjectHint) throws Exception {
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

    private boolean hasNonNull(JsonNode root, String field) {
        return root.has(field) && !root.get(field).isNull();
    }

    private boolean subjectIndicatesStatusChange(String subjectHint) {
        return subjectHint != null && subjectHint.contains("status");
    }
}

