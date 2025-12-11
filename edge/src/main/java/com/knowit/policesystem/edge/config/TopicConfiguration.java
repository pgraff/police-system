package com.knowit.policesystem.edge.config;

import org.springframework.stereotype.Component;

/**
 * Centralized configuration for Kafka topic names.
 * Provides a single source of truth for all topic names used throughout the system.
 * This ensures consistency and makes it easier to change topic names in the future.
 */
@Component
public class TopicConfiguration {

    public static final String OFFICER_EVENTS = "officer-events";
    public static final String VEHICLE_EVENTS = "vehicle-events";
    public static final String UNIT_EVENTS = "unit-events";
    public static final String PERSON_EVENTS = "person-events";
    public static final String LOCATION_EVENTS = "location-events";
    public static final String INCIDENT_EVENTS = "incident-events";
    public static final String CALL_EVENTS = "call-events";
    public static final String ACTIVITY_EVENTS = "activity-events";
    public static final String ASSIGNMENT_EVENTS = "assignment-events";
    public static final String SHIFT_EVENTS = "shift-events";
    public static final String OFFICER_SHIFT_EVENTS = "officer-shift-events";
    public static final String DISPATCH_EVENTS = "dispatch-events";
    public static final String RESOURCE_ASSIGNMENT_EVENTS = "resource-assignment-events";
    public static final String INVOLVED_PARTY_EVENTS = "involved-party-events";
}
