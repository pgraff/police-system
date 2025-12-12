package com.knowit.policesystem.projection.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.common.events.assignments.ChangeAssignmentStatusRequested;
import com.knowit.policesystem.common.events.assignments.CompleteAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.CreateAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.LinkAssignmentToDispatchRequested;
import com.knowit.policesystem.common.events.resourceassignment.AssignResourceRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AssignmentEventParser {

    private static final Logger log = LoggerFactory.getLogger(AssignmentEventParser.class);

    private final ObjectMapper objectMapper;

    public AssignmentEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Object parse(String payload, String subjectHint) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.has("eventType") ? root.get("eventType").asText() : null;

            if (eventType != null) {
                return parseByEventType(root, eventType);
            }

            return parseByHeuristics(root, subjectHint);
        } catch (Exception e) {
            log.error("Failed to parse assignment event", e);
            throw new IllegalArgumentException("Failed to parse assignment event", e);
        }
    }

    private Object parseByEventType(JsonNode root, String eventType) throws Exception {
        return switch (eventType) {
            case "CreateAssignmentRequested" -> objectMapper.treeToValue(root, CreateAssignmentRequested.class);
            case "ChangeAssignmentStatusRequested" -> objectMapper.treeToValue(root, ChangeAssignmentStatusRequested.class);
            case "CompleteAssignmentRequested" -> objectMapper.treeToValue(root, CompleteAssignmentRequested.class);
            case "LinkAssignmentToDispatchRequested" -> objectMapper.treeToValue(root, LinkAssignmentToDispatchRequested.class);
            case "AssignResourceRequested" -> objectMapper.treeToValue(root, AssignResourceRequested.class);
            default -> parseByHeuristics(root, null);
        };
    }

    private Object parseByHeuristics(JsonNode root, String subjectHint) throws Exception {
        boolean hasAssignedTime = hasNonNull(root, "assignedTime");
        boolean hasAssignmentType = hasNonNull(root, "assignmentType");
        boolean hasStatus = hasNonNull(root, "status");
        boolean hasCompletedTime = hasNonNull(root, "completedTime");
        boolean hasDispatchId = hasNonNull(root, "dispatchId");
        boolean hasResourceId = hasNonNull(root, "resourceId");
        boolean hasResourceType = hasNonNull(root, "resourceType");

        if (hasResourceId || hasResourceType) {
            return objectMapper.treeToValue(root, AssignResourceRequested.class);
        }

        if (hasDispatchId && !hasAssignedTime && !hasAssignmentType) {
            return objectMapper.treeToValue(root, LinkAssignmentToDispatchRequested.class);
        }

        if (hasCompletedTime && !hasAssignedTime) {
            return objectMapper.treeToValue(root, CompleteAssignmentRequested.class);
        }

        if (hasAssignedTime && hasAssignmentType && hasStatus) {
            return objectMapper.treeToValue(root, CreateAssignmentRequested.class);
        }

        if (hasStatus) {
            return objectMapper.treeToValue(root, ChangeAssignmentStatusRequested.class);
        }

        // Subject heuristic for status changes
        if (subjectIndicatesStatusChange(subjectHint)) {
            return objectMapper.treeToValue(root, ChangeAssignmentStatusRequested.class);
        }

        throw new IllegalArgumentException("Unknown assignment event payload");
    }

    private boolean hasNonNull(JsonNode root, String field) {
        return root.has(field) && !root.get(field).isNull();
    }

    private boolean subjectIndicatesStatusChange(String subjectHint) {
        return subjectHint != null && subjectHint.contains("status");
    }
}
