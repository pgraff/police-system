package com.knowit.policesystem.edge.commands.vehicles;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.vehicles.ChangeVehicleStatusRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.VehicleStatusResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ChangeVehicleStatusCommand.
 * Creates and publishes a ChangeVehicleStatusRequested event to Kafka and NATS/JetStream.
 */
@Component
public class ChangeVehicleStatusCommandHandler implements CommandHandler<ChangeVehicleStatusCommand, VehicleStatusResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new change vehicle status command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka and NATS/JetStream
     * @param registry the command handler registry for auto-registration
     */
    public ChangeVehicleStatusCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public VehicleStatusResponseDto handle(ChangeVehicleStatusCommand command) {
        // Create event from command
        ChangeVehicleStatusRequested event = new ChangeVehicleStatusRequested(
                command.getUnitId(),
                command.getStatus()
        );

        // Publish event to Kafka topic "vehicle-events"
        // DualEventPublisher will automatically also publish to NATS/JetStream subject "commands.vehicle.change-status"
        eventPublisher.publish("vehicle-events", command.getUnitId(), event);

        // Return response DTO
        return new VehicleStatusResponseDto(command.getUnitId(), command.getStatus());
    }

    @Override
    public Class<ChangeVehicleStatusCommand> getCommandType() {
        return ChangeVehicleStatusCommand.class;
    }
}
