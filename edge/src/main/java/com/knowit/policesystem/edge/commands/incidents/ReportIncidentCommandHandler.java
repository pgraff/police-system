package com.knowit.policesystem.edge.commands.incidents;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.edge.commands.Command;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.IncidentResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ReportIncidentCommand.
 * Creates and publishes a ReportIncidentRequested event to Kafka.
 */
@Component
public class ReportIncidentCommandHandler implements CommandHandler<ReportIncidentCommand, IncidentResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new report incident command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public ReportIncidentCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
        this.topicConfiguration = topicConfiguration;
    }

    /**
     * Registers this handler in the command handler registry.
     * Called after dependency injection is complete.
     */
    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public IncidentResponseDto handle(ReportIncidentCommand command) {
        // Create event from command
        ReportIncidentRequested event = new ReportIncidentRequested(
                command.getIncidentId(),
                command.getIncidentNumber(),
                EnumConverter.convertEnumToString(command.getPriority()),
                EnumConverter.convertEnumToString(command.getStatus()),
                command.getReportedTime(),
                command.getDescription(),
                EnumConverter.convertEnumToString(command.getIncidentType())
        );

        // Publish event to Kafka topic
        eventPublisher.publish(topicConfiguration.INCIDENT_EVENTS, command.getIncidentId(), event);

        // Return response DTO
        return new IncidentResponseDto(command.getIncidentId(), command.getIncidentNumber());
    }

    @Override
    public Class<ReportIncidentCommand> getCommandType() {
        return ReportIncidentCommand.class;
    }
}
