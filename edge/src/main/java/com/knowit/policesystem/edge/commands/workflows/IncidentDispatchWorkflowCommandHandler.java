package com.knowit.policesystem.edge.commands.workflows;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.assignments.CreateAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.LinkAssignmentToDispatchRequested;
import com.knowit.policesystem.common.events.resourceassignment.AssignResourceRequested;
import com.knowit.policesystem.common.events.calls.LinkCallToIncidentRequested;
import com.knowit.policesystem.common.events.incidents.DispatchIncidentRequested;
import com.knowit.policesystem.common.events.locations.LinkLocationToIncidentRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.IncidentDispatchWorkflowRequestDto;
import com.knowit.policesystem.edge.dto.IncidentDispatchWorkflowResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Command handler for IncidentDispatchWorkflowCommand.
 * Orchestrates multiple operations: create incident, dispatch, create assignment, assign resources.
 * Publishes multiple events in sequence.
 */
@Component
public class IncidentDispatchWorkflowCommandHandler implements CommandHandler<IncidentDispatchWorkflowCommand, IncidentDispatchWorkflowResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    public IncidentDispatchWorkflowCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
        this.topicConfiguration = topicConfiguration;
    }

    /**
     * Registers this handler in the command handler registry.
     */
    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public IncidentDispatchWorkflowResponseDto handle(IncidentDispatchWorkflowCommand command) {
        var requestDto = command.getRequestDto();
        var incidentData = requestDto.getIncident();
        String incidentId = incidentData.getIncidentId();

        // Generate IDs
        String dispatchId = "DISP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String assignmentId = requestDto.getAssignment() != null 
                ? requestDto.getAssignment().getAssignmentId() 
                : "ASSIGN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        List<String> resourceAssignmentIds = new ArrayList<>();

        // 1. Publish ReportIncidentRequested event
        ReportIncidentRequested reportEvent = new ReportIncidentRequested(
                incidentId,
                incidentData.getIncidentNumber(),
                EnumConverter.convertEnumToString(incidentData.getPriority()),
                EnumConverter.convertEnumToString(incidentData.getStatus()),
                incidentData.getReportedTime(),
                incidentData.getDescription(),
                EnumConverter.convertEnumToString(incidentData.getIncidentType())
        );
        eventPublisher.publish(topicConfiguration.INCIDENT_EVENTS, incidentId, reportEvent);

        // 2. Link location if provided
        if (requestDto.getLocationId() != null && !requestDto.getLocationId().isBlank()) {
            LinkLocationToIncidentRequested linkLocationEvent = new LinkLocationToIncidentRequested(
                    requestDto.getLocationId(),  // aggregateId
                    incidentId,
                    requestDto.getLocationId(),
                    "Primary",
                    null
            );
            eventPublisher.publish(topicConfiguration.INCIDENT_EVENTS, requestDto.getLocationId(), linkLocationEvent);
        }

        // 3. Link calls if provided
        if (requestDto.getCallIds() != null && !requestDto.getCallIds().isEmpty()) {
            for (String callId : requestDto.getCallIds()) {
                LinkCallToIncidentRequested linkCallEvent = new LinkCallToIncidentRequested(
                        callId,
                        callId,
                        incidentId
                );
                eventPublisher.publish(topicConfiguration.CALL_EVENTS, callId, linkCallEvent);
            }
        }

        // 4. Dispatch incident if dispatch data provided
        if (requestDto.getDispatch() != null && requestDto.getDispatch().getDispatchedTime() != null) {
            DispatchIncidentRequested dispatchEvent = new DispatchIncidentRequested(
                    incidentId,
                    requestDto.getDispatch().getDispatchedTime()
            );
            eventPublisher.publish(topicConfiguration.INCIDENT_EVENTS, incidentId, dispatchEvent);
        }

        // 5. Create assignment if assignment data provided
        if (requestDto.getAssignment() != null) {
            CreateAssignmentRequested assignmentEvent = new CreateAssignmentRequested(
                    assignmentId,
                    assignmentId,
                    requestDto.getAssignment().getAssignedTime(),
                    EnumConverter.convertEnumToString(requestDto.getAssignment().getAssignmentType()),
                    EnumConverter.convertStatusToString(requestDto.getAssignment().getStatus()),
                    incidentId,
                    null
            );
            eventPublisher.publish(topicConfiguration.ASSIGNMENT_EVENTS, assignmentId, assignmentEvent);

            // 6. Link assignment to dispatch if dispatch was created
            if (requestDto.getDispatch() != null && requestDto.getDispatch().getDispatchedTime() != null) {
                LinkAssignmentToDispatchRequested linkDispatchEvent = new LinkAssignmentToDispatchRequested(
                        assignmentId,
                        assignmentId,
                        dispatchId
                );
                eventPublisher.publish(topicConfiguration.ASSIGNMENT_EVENTS, assignmentId, linkDispatchEvent);
            }

            // 7. Assign resources if provided
            if (requestDto.getResources() != null && !requestDto.getResources().isEmpty()) {
                for (var resourceData : requestDto.getResources()) {
                    String resourceAssignmentId = UUID.randomUUID().toString();
                    resourceAssignmentIds.add(resourceAssignmentId);

                    AssignResourceRequested assignResourceEvent = new AssignResourceRequested(
                            assignmentId,  // aggregateId
                            assignmentId,
                            resourceData.getResourceId(),
                            EnumConverter.convertEnumToString(resourceData.getResourceType()),
                            EnumConverter.convertEnumToString(resourceData.getRoleType()),
                            resourceData.getStatus(),
                            resourceData.getStartTime()
                    );
                    eventPublisher.publish(topicConfiguration.RESOURCE_ASSIGNMENT_EVENTS, assignmentId, assignResourceEvent);
                }
            }
        }

        // Return comprehensive response
        return new IncidentDispatchWorkflowResponseDto(
                incidentId,
                dispatchId,
                assignmentId,
                resourceAssignmentIds
        );
    }

    @Override
    public Class<IncidentDispatchWorkflowCommand> getCommandType() {
        return IncidentDispatchWorkflowCommand.class;
    }
}
