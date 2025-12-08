package com.knowit.policesystem.edge.commands.incidents;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.domain.IncidentStatus;
import com.knowit.policesystem.edge.domain.IncidentType;
import com.knowit.policesystem.edge.domain.Priority;
import com.knowit.policesystem.edge.dto.ReportIncidentRequestDto;

import java.time.Instant;

/**
 * Command for reporting an incident.
 * This command is processed by ReportIncidentCommandHandler.
 */
public class ReportIncidentCommand extends Command {

    private String incidentId;
    private String incidentNumber;
    private Priority priority;
    private IncidentStatus status;
    private Instant reportedTime;
    private String description;
    private IncidentType incidentType;

    /**
     * Default constructor for deserialization.
     */
    public ReportIncidentCommand() {
        super();
    }

    /**
     * Creates a new report incident command from a DTO.
     *
     * @param aggregateId the aggregate identifier (incidentId)
     * @param dto the request DTO containing incident data
     */
    public ReportIncidentCommand(String aggregateId, ReportIncidentRequestDto dto) {
        super(aggregateId);
        this.incidentId = dto.getIncidentId();
        this.incidentNumber = dto.getIncidentNumber();
        this.priority = dto.getPriority();
        this.status = dto.getStatus();
        this.reportedTime = dto.getReportedTime();
        this.description = dto.getDescription();
        this.incidentType = dto.getIncidentType();
    }

    @Override
    public String getCommandType() {
        return "ReportIncidentCommand";
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

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
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

    public IncidentType getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(IncidentType incidentType) {
        this.incidentType = incidentType;
    }
}
