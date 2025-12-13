package com.knowit.policesystem.projection.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowit.policesystem.common.events.locations.CreateLocationRequested;
import com.knowit.policesystem.common.events.locations.UpdateLocationRequested;
import com.knowit.policesystem.common.events.officers.ChangeOfficerStatusRequested;
import com.knowit.policesystem.common.events.officers.RegisterOfficerRequested;
import com.knowit.policesystem.common.events.officers.UpdateOfficerRequested;
import com.knowit.policesystem.common.events.persons.RegisterPersonRequested;
import com.knowit.policesystem.common.events.persons.UpdatePersonRequested;
import com.knowit.policesystem.common.events.units.ChangeUnitStatusRequested;
import com.knowit.policesystem.common.events.units.CreateUnitRequested;
import com.knowit.policesystem.common.events.units.UpdateUnitRequested;
import com.knowit.policesystem.common.events.vehicles.ChangeVehicleStatusRequested;
import com.knowit.policesystem.common.events.vehicles.RegisterVehicleRequested;
import com.knowit.policesystem.common.events.vehicles.UpdateVehicleRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ResourceEventParser {

    private static final Logger log = LoggerFactory.getLogger(ResourceEventParser.class);

    private final ObjectMapper objectMapper;

    public ResourceEventParser(ObjectMapper objectMapper) {
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
            log.error("Failed to parse resource event", e);
            throw new IllegalArgumentException("Failed to parse resource event", e);
        }
    }

    private Object parseByEventType(JsonNode root, String eventType) throws Exception {
        return switch (eventType) {
            // Officer events
            case "RegisterOfficerRequested" -> objectMapper.treeToValue(root, RegisterOfficerRequested.class);
            case "UpdateOfficerRequested" -> objectMapper.treeToValue(root, UpdateOfficerRequested.class);
            case "ChangeOfficerStatusRequested" -> objectMapper.treeToValue(root, ChangeOfficerStatusRequested.class);
            
            // Vehicle events
            case "RegisterVehicleRequested" -> objectMapper.treeToValue(root, RegisterVehicleRequested.class);
            case "UpdateVehicleRequested" -> objectMapper.treeToValue(root, UpdateVehicleRequested.class);
            case "ChangeVehicleStatusRequested" -> objectMapper.treeToValue(root, ChangeVehicleStatusRequested.class);
            
            // Unit events
            case "CreateUnitRequested" -> objectMapper.treeToValue(root, CreateUnitRequested.class);
            case "UpdateUnitRequested" -> objectMapper.treeToValue(root, UpdateUnitRequested.class);
            case "ChangeUnitStatusRequested" -> objectMapper.treeToValue(root, ChangeUnitStatusRequested.class);
            
            // Person events
            case "RegisterPersonRequested" -> objectMapper.treeToValue(root, RegisterPersonRequested.class);
            case "UpdatePersonRequested" -> objectMapper.treeToValue(root, UpdatePersonRequested.class);
            
            // Location events
            case "CreateLocationRequested" -> objectMapper.treeToValue(root, CreateLocationRequested.class);
            case "UpdateLocationRequested" -> objectMapper.treeToValue(root, UpdateLocationRequested.class);
            
            default -> parseByHeuristics(root, null);
        };
    }

    private Object parseByHeuristics(JsonNode root, String subjectHint) throws Exception {
        // Determine domain from subject hint or field presence
        String domain = determineDomain(root, subjectHint);
        
        return switch (domain) {
            case "officer" -> parseOfficerEvent(root, subjectHint);
            case "vehicle" -> parseVehicleEvent(root, subjectHint);
            case "unit" -> parseUnitEvent(root, subjectHint);
            case "person" -> parsePersonEvent(root, subjectHint);
            case "location" -> parseLocationEvent(root, subjectHint);
            default -> throw new IllegalArgumentException("Unknown resource event domain: " + domain);
        };
    }

    private String determineDomain(JsonNode root, String subjectHint) {
        // Try subject hint first
        if (subjectHint != null) {
            if (subjectHint.contains("officer")) return "officer";
            if (subjectHint.contains("vehicle")) return "vehicle";
            if (subjectHint.contains("unit")) return "unit";
            if (subjectHint.contains("person")) return "person";
            if (subjectHint.contains("location")) return "location";
        }
        
        // Fallback to field presence - check most specific fields first
        // Officer has badgeNumber
        if (hasNonNull(root, "badgeNumber")) {
            return "officer";
        }
        // Vehicle has vehicleType, licensePlate, vin, lastMaintenanceDate - check before unit
        if (hasNonNull(root, "vehicleType") || hasNonNull(root, "licensePlate") || hasNonNull(root, "vin") 
                || hasNonNull(root, "lastMaintenanceDate")) {
            return "vehicle";
        }
        // Unit has unitType (but no vehicle-specific fields)
        if (hasNonNull(root, "unitType")) {
            return "unit";
        }
        // Person has dateOfBirth, gender, race
        if (hasNonNull(root, "dateOfBirth") || hasNonNull(root, "gender") || hasNonNull(root, "race")) {
            return "person";
        }
        // Location has address, city, state, latitude, longitude
        if (hasNonNull(root, "address") || hasNonNull(root, "city") || hasNonNull(root, "state") 
                || hasNonNull(root, "latitude") || hasNonNull(root, "longitude")) {
            return "location";
        }
        // Fallback to ID fields (less specific)
        if (hasNonNull(root, "locationId")) {
            return "location";
        }
        if (hasNonNull(root, "personId")) {
            return "person";
        }
        if (hasNonNull(root, "unitId")) {
            // Could be vehicle or unit - check for vehicle-specific fields first
            if (hasNonNull(root, "vehicleType") || hasNonNull(root, "licensePlate") || hasNonNull(root, "vin")) {
                return "vehicle";
            }
            // If only status is present, check if it's a status change event
            // Status change events for vehicles and units look similar, but vehicle has more context
            // For now, default to unit if no vehicle-specific fields
            return "unit";
        }
        
        throw new IllegalArgumentException("Cannot determine domain for resource event");
    }

    private Object parseOfficerEvent(JsonNode root, String subjectHint) throws Exception {
        boolean hasStatus = hasNonNull(root, "status");
        boolean hasOfficerFields = hasNonNull(root, "firstName") || hasNonNull(root, "lastName")
                || hasNonNull(root, "rank") || hasNonNull(root, "email") || hasNonNull(root, "hireDate");

        // ChangeOfficerStatusRequested has only status
        if (hasStatus && !hasOfficerFields) {
            return objectMapper.treeToValue(root, ChangeOfficerStatusRequested.class);
        }
        if (subjectIndicatesStatusChange(subjectHint)) {
            return objectMapper.treeToValue(root, ChangeOfficerStatusRequested.class);
        }

        // RegisterOfficerRequested has hireDate and badgeNumber
        if (hasNonNull(root, "hireDate") && hasNonNull(root, "badgeNumber")) {
            return objectMapper.treeToValue(root, RegisterOfficerRequested.class);
        }

        // UpdateOfficerRequested has officer fields but no hireDate
        if (hasOfficerFields) {
            return objectMapper.treeToValue(root, UpdateOfficerRequested.class);
        }

        throw new IllegalArgumentException("Unknown officer event payload");
    }

    private Object parseVehicleEvent(JsonNode root, String subjectHint) throws Exception {
        boolean hasStatus = hasNonNull(root, "status");
        boolean hasVehicleFields = hasNonNull(root, "vehicleType") || hasNonNull(root, "licensePlate")
                || hasNonNull(root, "vin") || hasNonNull(root, "lastMaintenanceDate");

        // ChangeVehicleStatusRequested has only status (and unitId)
        if (hasStatus && !hasVehicleFields) {
            return objectMapper.treeToValue(root, ChangeVehicleStatusRequested.class);
        }
        if (subjectIndicatesStatusChange(subjectHint)) {
            return objectMapper.treeToValue(root, ChangeVehicleStatusRequested.class);
        }

        // RegisterVehicleRequested has vehicleType, licensePlate, vin, status
        if (hasNonNull(root, "vehicleType") && hasNonNull(root, "licensePlate") && hasNonNull(root, "vin") && hasStatus) {
            return objectMapper.treeToValue(root, RegisterVehicleRequested.class);
        }

        // UpdateVehicleRequested has vehicle fields but may not have all required for register
        if (hasVehicleFields) {
            return objectMapper.treeToValue(root, UpdateVehicleRequested.class);
        }

        throw new IllegalArgumentException("Unknown vehicle event payload");
    }

    private Object parseUnitEvent(JsonNode root, String subjectHint) throws Exception {
        boolean hasStatus = hasNonNull(root, "status");
        boolean hasUnitType = hasNonNull(root, "unitType");

        // ChangeUnitStatusRequested has only status
        if (hasStatus && !hasUnitType) {
            return objectMapper.treeToValue(root, ChangeUnitStatusRequested.class);
        }
        if (subjectIndicatesStatusChange(subjectHint)) {
            return objectMapper.treeToValue(root, ChangeUnitStatusRequested.class);
        }

        // CreateUnitRequested has unitType and status
        if (hasUnitType && hasStatus) {
            return objectMapper.treeToValue(root, CreateUnitRequested.class);
        }

        // UpdateUnitRequested has unitType or status but not both
        if (hasUnitType || hasStatus) {
            return objectMapper.treeToValue(root, UpdateUnitRequested.class);
        }

        throw new IllegalArgumentException("Unknown unit event payload");
    }

    private Object parsePersonEvent(JsonNode root, String subjectHint) throws Exception {
        boolean hasPersonFields = hasNonNull(root, "firstName") || hasNonNull(root, "lastName")
                || hasNonNull(root, "dateOfBirth") || hasNonNull(root, "gender") || hasNonNull(root, "race");

        // RegisterPersonRequested has dateOfBirth, firstName, lastName
        if (hasNonNull(root, "dateOfBirth") && hasNonNull(root, "firstName") && hasNonNull(root, "lastName")) {
            return objectMapper.treeToValue(root, RegisterPersonRequested.class);
        }

        // UpdatePersonRequested has person fields
        if (hasPersonFields) {
            return objectMapper.treeToValue(root, UpdatePersonRequested.class);
        }

        throw new IllegalArgumentException("Unknown person event payload");
    }

    private Object parseLocationEvent(JsonNode root, String subjectHint) throws Exception {
        boolean hasLocationFields = hasNonNull(root, "address") || hasNonNull(root, "city")
                || hasNonNull(root, "state") || hasNonNull(root, "zipCode")
                || hasNonNull(root, "latitude") || hasNonNull(root, "longitude") || hasNonNull(root, "locationType");

        // Check subject hint first
        if (subjectHint != null) {
            if (subjectHint.contains("create") || subjectHint.contains("register")) {
                return objectMapper.treeToValue(root, CreateLocationRequested.class);
            }
            if (subjectHint.contains("update")) {
                return objectMapper.treeToValue(root, UpdateLocationRequested.class);
            }
        }

        // Count non-null location fields
        int fieldCount = 0;
        if (hasNonNull(root, "address")) fieldCount++;
        if (hasNonNull(root, "city")) fieldCount++;
        if (hasNonNull(root, "state")) fieldCount++;
        if (hasNonNull(root, "zipCode")) fieldCount++;
        if (hasNonNull(root, "latitude")) fieldCount++;
        if (hasNonNull(root, "longitude")) fieldCount++;
        if (hasNonNull(root, "locationType")) fieldCount++;

        // CreateLocationRequested typically has more complete location data (3+ fields)
        if (hasNonNull(root, "locationId") && fieldCount >= 3) {
            return objectMapper.treeToValue(root, CreateLocationRequested.class);
        }

        // UpdateLocationRequested has location fields but fewer (partial update)
        if (hasLocationFields) {
            return objectMapper.treeToValue(root, UpdateLocationRequested.class);
        }

        throw new IllegalArgumentException("Unknown location event payload");
    }

    private boolean hasNonNull(JsonNode root, String field) {
        return root.has(field) && !root.get(field).isNull();
    }

    private boolean subjectIndicatesStatusChange(String subjectHint) {
        return subjectHint != null && subjectHint.contains("status");
    }
}
