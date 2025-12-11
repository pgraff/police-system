package com.knowit.policesystem.edge.commands.units;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.units.UpdateUnitRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.UnitResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for UpdateUnitCommand.
 * Creates and publishes an UpdateUnitRequested event to Kafka and NATS/JetStream.
 */
@Component
public class UpdateUnitCommandHandler implements CommandHandler<UpdateUnitCommand, UnitResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new update unit command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka and NATS/JetStream
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public UpdateUnitCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public UnitResponseDto handle(UpdateUnitCommand command) {
        // Create event from command
        // Only include fields that were provided (nulls for omitted fields support partial updates)
        UpdateUnitRequested event = new UpdateUnitRequested(
                command.getUnitId(),
                EnumConverter.convertEnumToString(command.getUnitType()),
                EnumConverter.convertStatusToString(command.getStatus())
        );

        // Publish event to Kafka topic
        // DualEventPublisher will automatically also publish to NATS/JetStream subject "commands.unit.update"
        eventPublisher.publish(topicConfiguration.UNIT_EVENTS, command.getUnitId(), event);

        // Return response DTO
        return new UnitResponseDto(command.getUnitId());
    }

    @Override
    public Class<UpdateUnitCommand> getCommandType() {
        return UpdateUnitCommand.class;
    }
}
