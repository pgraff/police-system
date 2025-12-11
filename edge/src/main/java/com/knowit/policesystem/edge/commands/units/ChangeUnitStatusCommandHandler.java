package com.knowit.policesystem.edge.commands.units;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.units.ChangeUnitStatusRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.UnitStatusResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ChangeUnitStatusCommand.
 * Creates and publishes a ChangeUnitStatusRequested event to Kafka and NATS/JetStream.
 */
@Component
public class ChangeUnitStatusCommandHandler implements CommandHandler<ChangeUnitStatusCommand, UnitStatusResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new change unit status command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka and NATS/JetStream
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public ChangeUnitStatusCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public UnitStatusResponseDto handle(ChangeUnitStatusCommand command) {
        // Create event from command
        ChangeUnitStatusRequested event = new ChangeUnitStatusRequested(
                command.getUnitId(),
                command.getStatus()
        );

        // Publish event to Kafka topic
        // DualEventPublisher will automatically also publish to NATS/JetStream subject "commands.unit.change-status"
        eventPublisher.publish(topicConfiguration.UNIT_EVENTS, command.getUnitId(), event);

        // Return response DTO
        return new UnitStatusResponseDto(command.getUnitId(), command.getStatus());
    }

    @Override
    public Class<ChangeUnitStatusCommand> getCommandType() {
        return ChangeUnitStatusCommand.class;
    }
}
