package com.knowit.policesystem.edge.commands.incidents;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.calls.LinkCallToIncidentRequested;
import com.knowit.policesystem.common.events.locations.LinkLocationToIncidentRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.IncidentResponseDto;
import com.knowit.policesystem.edge.dto.RelatedResourcesDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Command handler for CreateIncidentWithRelationsCommand.
 * Publishes multiple events: ReportIncidentRequested, LinkLocationToIncidentRequested, LinkCallToIncidentRequested.
 */
@Component
public class CreateIncidentWithRelationsCommandHandler implements CommandHandler<CreateIncidentWithRelationsCommand, IncidentResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    public CreateIncidentWithRelationsCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public IncidentResponseDto handle(CreateIncidentWithRelationsCommand command) {
        var requestDto = command.getRequestDto();
        String incidentId = command.getAggregateId();

        // Publish ReportIncidentRequested event
        ReportIncidentRequested reportEvent = new ReportIncidentRequested(
                incidentId,
                requestDto.getIncidentNumber(),
                requestDto.getPriority() != null ? requestDto.getPriority().name() : null,
                requestDto.getStatus() != null ? requestDto.getStatus().name() : null,
                requestDto.getReportedTime(),
                requestDto.getDescription(),
                requestDto.getIncidentType() != null ? requestDto.getIncidentType().name() : null
        );
        eventPublisher.publish(topicConfiguration.INCIDENT_EVENTS, incidentId, reportEvent);

        // Publish LinkLocationToIncidentRequested if locationId provided
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

        // Publish LinkCallToIncidentRequested for each callId
        if (requestDto.getCallIds() != null && !requestDto.getCallIds().isEmpty()) {
            for (String callId : requestDto.getCallIds()) {
                LinkCallToIncidentRequested linkCallEvent = new LinkCallToIncidentRequested(
                        callId,  // aggregateId
                        callId,
                        incidentId
                );
                eventPublisher.publish(topicConfiguration.CALL_EVENTS, callId, linkCallEvent);
            }
        }

        // Create response with related resources
        IncidentResponseDto response = new IncidentResponseDto(incidentId, requestDto.getIncidentNumber());
        RelatedResourcesDto relatedResources = new RelatedResourcesDto();
        relatedResources.setLocationId(requestDto.getLocationId());
        relatedResources.setCallIds(requestDto.getCallIds());
        response.setRelatedResources(relatedResources);

        return response;
    }

    @Override
    public Class<CreateIncidentWithRelationsCommand> getCommandType() {
        return CreateIncidentWithRelationsCommand.class;
    }
}
