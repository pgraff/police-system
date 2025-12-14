package com.knowit.policesystem.edge.commands.incidents;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.incidents.DispatchIncidentRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.DispatchIncidentResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Command handler for DispatchIncidentCommand.
 * Publishes DispatchIncidentRequested events using the dual publisher (Kafka + NATS for critical events).
 * Returns dispatch ID for UI convenience.
 */
@Component
public class DispatchIncidentCommandHandler implements CommandHandler<DispatchIncidentCommand, DispatchIncidentResponseDto> {

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
    public DispatchIncidentResponseDto handle(DispatchIncidentCommand command) {
        // Generate dispatch ID for UI convenience (will be used when creating dispatch entity)
        String dispatchId = "DISP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        DispatchIncidentRequested event = new DispatchIncidentRequested(
                command.getIncidentId(),
                command.getDispatchedTime()
        );

        // Publish event to Kafka topic (DualEventPublisher will also publish to NATS for critical events)
        eventPublisher.publish(topicConfiguration.INCIDENT_EVENTS, command.getIncidentId(), event);

        return new DispatchIncidentResponseDto(
                command.getIncidentId(),
                null,
                dispatchId,
                command.getDispatchedTime()
        );
    }

    @Override
    public Class<DispatchIncidentCommand> getCommandType() {
        return DispatchIncidentCommand.class;
    }
}
