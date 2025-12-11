package com.knowit.policesystem.projection.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.common.events.officers.ChangeOfficerStatusRequested;
import com.knowit.policesystem.common.events.officers.RegisterOfficerRequested;
import com.knowit.policesystem.common.events.officers.UpdateOfficerRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OfficerEventParser {

    private static final Logger log = LoggerFactory.getLogger(OfficerEventParser.class);

    private final ObjectMapper objectMapper;

    public OfficerEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Object parse(String payload, String subjectHint) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            boolean hasStatus = root.has("status") && !root.get("status").isNull();
            boolean hasOfficerFields = hasNonNull(root, "firstName")
                    || hasNonNull(root, "lastName")
                    || hasNonNull(root, "rank")
                    || hasNonNull(root, "email")
                    || hasNonNull(root, "phoneNumber")
                    || hasNonNull(root, "hireDate");

            if (subjectIndicatesStatusChange(subjectHint)) {
                return objectMapper.treeToValue(root, ChangeOfficerStatusRequested.class);
            }

            if (hasOfficerFields && hasStatus) {
                return objectMapper.treeToValue(root, RegisterOfficerRequested.class);
            }

            if (hasOfficerFields) {
                return objectMapper.treeToValue(root, UpdateOfficerRequested.class);
            }

            if (hasStatus) {
                return objectMapper.treeToValue(root, ChangeOfficerStatusRequested.class);
            }

            throw new IllegalArgumentException("Unknown officer event payload");
        } catch (Exception e) {
            log.error("Failed to parse officer event", e);
            throw new IllegalArgumentException("Failed to parse officer event", e);
        }
    }

    private boolean hasNonNull(JsonNode root, String field) {
        return root.has(field) && !root.get(field).isNull();
    }

    private boolean subjectIndicatesStatusChange(String subjectHint) {
        return subjectHint != null && subjectHint.contains("status");
    }
}
