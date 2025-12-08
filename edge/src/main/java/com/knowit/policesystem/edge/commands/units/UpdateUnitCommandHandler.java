package com.knowit.policesystem.edge.commands.units;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.units.UpdateUnitRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.UnitResponseDto;
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

    /**
     * Creates a new update unit command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka and NATS/JetStream
     * @param registry the command handler registry for auto-registration
     */
    public UpdateUnitCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
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
                command.getUnitType() != null ? command.getUnitType().name() : null,
                command.getStatus() != null ? command.getStatus().name() : null
        );

        // Publish event to Kafka topic "unit-events"
        // DualEventPublisher will automatically also publish to NATS/JetStream subject "commands.unit.update"
        eventPublisher.publish("unit-events", command.getUnitId(), event);

        // Return response DTO
        return new UnitResponseDto(command.getUnitId());
    }

    @Override
    public Class<UpdateUnitCommand> getCommandType() {
        return UpdateUnitCommand.class;
    }
}
