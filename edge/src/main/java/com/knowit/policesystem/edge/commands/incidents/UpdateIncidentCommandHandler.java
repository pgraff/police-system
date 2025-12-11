package com.knowit.policesystem.edge.commands.incidents;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.incidents.UpdateIncidentRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.IncidentResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for UpdateIncidentCommand.
 * Publishes UpdateIncidentRequested via dual publisher (Kafka + NATS for critical events).
 */
@Component
public class UpdateIncidentCommandHandler implements CommandHandler<UpdateIncidentCommand, IncidentResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    public UpdateIncidentCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
        this.topicConfiguration = topicConfiguration;
    }

    /** Registers this handler in the command handler registry. */
    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public IncidentResponseDto handle(UpdateIncidentCommand command) {
        UpdateIncidentRequested event = new UpdateIncidentRequested(
                command.getIncidentId(),
                EnumConverter.convertEnumToString(command.getPriority()),
                command.getDescription(),
                EnumConverter.convertEnumToString(command.getIncidentType())
        );

        // Publish to Kafka topic; DualEventPublisher will also publish to NATS for critical events.
        eventPublisher.publish(topicConfiguration.INCIDENT_EVENTS, command.getIncidentId(), event);

        return new IncidentResponseDto(command.getIncidentId(), null);
    }

    @Override
    public Class<UpdateIncidentCommand> getCommandType() {
        return UpdateIncidentCommand.class;
    }
}
