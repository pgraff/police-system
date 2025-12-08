package com.knowit.policesystem.edge.commands.units;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.units.CreateUnitRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.UnitResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for CreateUnitCommand.
 * Creates and publishes a CreateUnitRequested event to Kafka and NATS/JetStream.
 */
@Component
public class CreateUnitCommandHandler implements CommandHandler<CreateUnitCommand, UnitResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new create unit command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka and NATS/JetStream
     * @param registry the command handler registry for auto-registration
     */
    public CreateUnitCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public UnitResponseDto handle(CreateUnitCommand command) {
        // Create event from command
        CreateUnitRequested event = new CreateUnitRequested(
                command.getUnitId(),
                command.getUnitType() != null ? command.getUnitType().name() : null,
                command.getStatus() != null ? command.getStatus().name() : null
        );

        // Publish event to Kafka topic "unit-events"
        // DualEventPublisher will automatically also publish to NATS/JetStream subject "commands.unit.create"
        eventPublisher.publish("unit-events", command.getUnitId(), event);

        // Return response DTO
        return new UnitResponseDto(command.getUnitId());
    }

    @Override
    public Class<CreateUnitCommand> getCommandType() {
        return CreateUnitCommand.class;
    }
}
