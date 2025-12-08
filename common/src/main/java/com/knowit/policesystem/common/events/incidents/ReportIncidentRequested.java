package com.knowit.policesystem.common.events.incidents;

import com.knowit.policesystem.common.events.Event;

import java.time.Instant;

/**
 * Event representing a request to report an incident.
 * This event is published to Kafka when an incident is reported via the REST API.
 * Follows the event-driven architecture pattern where events represent requests/commands.
 */
public class ReportIncidentRequested extends Event {

    private String incidentId;
    private String incidentNumber;
    private String priority;
    private String status;
    private Instant reportedTime;
    private String description;
    private String incidentType;

    /**
     * Default constructor for deserialization.
     */
    public ReportIncidentRequested() {
        super();
    }

    /**
     * Creates a new ReportIncidentRequested event.
     *
     * @param incidentId the incident identifier (used as aggregateId)
     * @param incidentNumber the incident number
     * @param priority the priority as string
     * @param status the status as string
     * @param reportedTime the reported time
     * @param description the description
     * @param incidentType the incident type as string
     */
    public ReportIncidentRequested(String incidentId, String incidentNumber, String priority,
                                   String status, Instant reportedTime, String description,
                                   String incidentType) {
        super(incidentId);
        this.incidentId = incidentId;
        this.incidentNumber = incidentNumber;
        this.priority = priority;
        this.status = status;
        this.reportedTime = reportedTime;
        this.description = description;
        this.incidentType = incidentType;
    }

    @Override
    public String getEventType() {
        return "ReportIncidentRequested";
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public void setIncidentNumber(String incidentNumber) {
        this.incidentNumber = incidentNumber;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getReportedTime() {
        return reportedTime;
    }

    public void setReportedTime(Instant reportedTime) {
        this.reportedTime = reportedTime;
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
