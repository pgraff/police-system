package com.knowit.policesystem.edge.commands.incidents;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.incidents.DispatchIncidentRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.IncidentResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for DispatchIncidentCommand.
 * Publishes DispatchIncidentRequested events using the dual publisher (Kafka + NATS for critical events).
 */
@Component
public class DispatchIncidentCommandHandler implements CommandHandler<DispatchIncidentCommand, IncidentResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    public DispatchIncidentCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public IncidentResponseDto handle(DispatchIncidentCommand command) {
        DispatchIncidentRequested event = new DispatchIncidentRequested(
                command.getIncidentId(),
                command.getDispatchedTime()
        );

        // Publish event to Kafka topic (DualEventPublisher will also publish to NATS for critical events)
        eventPublisher.publish(topicConfiguration.INCIDENT_EVENTS, command.getIncidentId(), event);

        return new IncidentResponseDto(command.getIncidentId(), null);
    }

    @Override
    public Class<DispatchIncidentCommand> getCommandType() {
        return DispatchIncidentCommand.class;
    }
}
