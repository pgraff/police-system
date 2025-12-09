package com.knowit.policesystem.common.events.incidents;

import com.knowit.policesystem.common.events.Event;

/**
 * Event representing a request to update an incident.
 * Published via DualEventPublisher to Kafka (and NATS/JetStream for critical events).
 */
public class UpdateIncidentRequested extends Event {

    private String incidentId;
    private String priority;
    private String description;
    private String incidentType;

    /** Default constructor for deserialization. */
    public UpdateIncidentRequested() {
        super();
    }

    /**
     * Creates a new UpdateIncidentRequested event.
     *
     * @param incidentId the incident identifier (aggregateId)
     * @param priority the priority level (nullable)
     * @param description the incident description (nullable)
     * @param incidentType the type of incident (nullable)
     */
    public UpdateIncidentRequested(String incidentId, String priority, String description, String incidentType) {
        super(incidentId);
        this.incidentId = incidentId;
        this.priority = priority;
        this.description = description;
        this.incidentType = incidentType;
    }

    @Override
    public String getEventType() {
        return "UpdateIncidentRequested";
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }
}
