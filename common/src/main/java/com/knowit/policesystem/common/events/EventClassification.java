package com.knowit.policesystem.common.events;

/**
 * Utility class for classifying events as critical or non-critical.
 * Critical events require near realtime processing and are published to both Kafka and NATS/JetStream.
 * Non-critical events are published only to Kafka.
 */
public class EventClassification {

    /**
     * Determines if an event is critical and should be published to NATS/JetStream.
     * 
     * Critical events include:
     * - All command events (events ending with "Requested")
     * - Status change events requiring immediate processing
     * - Dispatch and assignment events
     * 
     * @param event the event to classify
     * @return true if the event is critical, false otherwise
     */
    public static boolean isCritical(Event event) {
        if (event == null) {
            return false;
        }
        
        String eventType = event.getEventType();
        if (eventType == null) {
            return false;
        }
        
        // All command events (ending with "Requested") are considered critical
        return eventType.endsWith("Requested");
    }

    /**
     * Generates a NATS JetStream subject name for a critical event.
     * Subject naming convention: commands.{domain}.{action}
     * 
     * Example: ReportIncidentRequested -> commands.incident.report
     * Example: RegisterOfficerRequested -> commands.officer.register
     * 
     * @param event the event to generate a subject for
     * @return the NATS JetStream subject name
     */
    public static String generateNatsSubject(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        
        String eventType = event.getEventType();
        if (eventType == null || eventType.isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        
        // Remove "Requested" suffix if present
        String baseName = eventType;
        if (baseName.endsWith("Requested")) {
            baseName = baseName.substring(0, baseName.length() - "Requested".length());
        }
        
        // Convert from PascalCase to kebab-case
        // Example: ReportIncident -> report-incident
        String kebabCase = convertToKebabCase(baseName);
        
        // Extract domain from event type
        // For now, we'll use a simple heuristic: extract domain from common patterns
        String domain = extractDomain(eventType);
        
        return "commands." + domain + "." + kebabCase;
    }

    /**
     * Converts PascalCase to kebab-case.
     * Example: ReportIncident -> report-incident
     */
    private static String convertToKebabCase(String pascalCase) {
        if (pascalCase == null || pascalCase.isEmpty()) {
            return pascalCase;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < pascalCase.length(); i++) {
            char c = pascalCase.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('-');
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }

    /**
     * Extracts the domain from an event type.
     * Examples:
     * - ReportIncidentRequested -> incident
     * - RegisterOfficerRequested -> officer
     * - CreateUnitRequested -> unit
     */
    private static String extractDomain(String eventType) {
        if (eventType == null || eventType.isEmpty()) {
            return "unknown";
        }
        
        // Remove "Requested" suffix
        String baseName = eventType;
        if (baseName.endsWith("Requested")) {
            baseName = baseName.substring(0, baseName.length() - "Requested".length());
        }
        
        // Common domain patterns
        String[] domainKeywords = {
            "Incident", "Officer", "Vehicle", "Unit", "Person", "Location",
            "Call", "Activity", "Assignment", "Shift", "Dispatch", "Resource",
            "Party", "Involved"
        };
        
        for (String keyword : domainKeywords) {
            if (baseName.contains(keyword)) {
                return keyword.toLowerCase();
            }
        }
        
        // Fallback: use first word in lowercase
        // Find first capital letter after the first one
        int firstCap = 0;
        for (int i = 1; i < baseName.length(); i++) {
            if (Character.isUpperCase(baseName.charAt(i))) {
                firstCap = i;
                break;
            }
        }
        
        if (firstCap > 0) {
            String firstWord = baseName.substring(0, firstCap);
            return firstWord.toLowerCase();
        }
        
        return baseName.toLowerCase();
    }
}
