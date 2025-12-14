package com.knowit.policesystem.edge.validation.workflows;

import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.workflows.IncidentDispatchWorkflowCommand;
import com.knowit.policesystem.edge.dto.IncidentDispatchWorkflowRequestDto;
import com.knowit.policesystem.edge.validation.CommandValidator;
import com.knowit.policesystem.edge.validation.ValidationError;
import com.knowit.policesystem.edge.validation.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validator for IncidentDispatchWorkflowCommand.
 * Validates that all required fields in the workflow request are present.
 */
@Component
public class IncidentDispatchWorkflowCommandValidator extends CommandValidator {

    @Override
    public ValidationResult validate(Command command) {
        if (!(command instanceof IncidentDispatchWorkflowCommand)) {
            return ValidationResult.valid();
        }

        IncidentDispatchWorkflowCommand workflowCommand = (IncidentDispatchWorkflowCommand) command;
        var requestDto = workflowCommand.getRequestDto();
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate incident data
        if (requestDto.getIncident() == null) {
            builder.addError(new ValidationError("incident", "incident is required", null));
        } else {
            var incident = requestDto.getIncident();
            if (incident.getIncidentId() == null || incident.getIncidentId().trim().isEmpty()) {
                builder.addError(new ValidationError("incident.incidentId", "incident.incidentId is required", null));
            }
            if (incident.getPriority() == null) {
                builder.addError(new ValidationError("incident.priority", "incident.priority is required", null));
            }
            if (incident.getStatus() == null) {
                builder.addError(new ValidationError("incident.status", "incident.status is required", null));
            }
            if (incident.getIncidentType() == null) {
                builder.addError(new ValidationError("incident.incidentType", "incident.incidentType is required", null));
            }
        }

        // Validate assignment data if provided
        if (requestDto.getAssignment() != null) {
            var assignment = requestDto.getAssignment();
            if (assignment.getAssignmentId() == null || assignment.getAssignmentId().trim().isEmpty()) {
                builder.addError(new ValidationError("assignment.assignmentId", "assignment.assignmentId is required", null));
            }
            if (assignment.getBadgeNumber() == null || assignment.getBadgeNumber().trim().isEmpty()) {
                builder.addError(new ValidationError("assignment.badgeNumber", "assignment.badgeNumber is required", null));
            }
            if (assignment.getAssignmentType() == null) {
                builder.addError(new ValidationError("assignment.assignmentType", "assignment.assignmentType is required", null));
            }
            if (assignment.getStatus() == null) {
                builder.addError(new ValidationError("assignment.status", "assignment.status is required", null));
            }
        }

        // Validate resources if provided
        if (requestDto.getResources() != null) {
            for (int i = 0; i < requestDto.getResources().size(); i++) {
                var resource = requestDto.getResources().get(i);
                String prefix = "resources[" + i + "]";
                if (resource.getResourceId() == null || resource.getResourceId().trim().isEmpty()) {
                    builder.addError(new ValidationError(prefix + ".resourceId", prefix + ".resourceId is required", null));
                }
                if (resource.getResourceType() == null) {
                    builder.addError(new ValidationError(prefix + ".resourceType", prefix + ".resourceType is required", null));
                }
                if (resource.getRoleType() == null) {
                    builder.addError(new ValidationError(prefix + ".roleType", prefix + ".roleType is required", null));
                }
                if (resource.getStatus() == null || resource.getStatus().trim().isEmpty()) {
                    builder.addError(new ValidationError(prefix + ".status", prefix + ".status is required", null));
                }
            }
        }

        return builder.build();
    }
}
