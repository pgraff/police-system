package com.knowit.policesystem.edge.commands.vehicles;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.vehicles.UpdateVehicleRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.VehicleResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for UpdateVehicleCommand.
 * Creates and publishes an UpdateVehicleRequested event to Kafka and NATS/JetStream.
 */
@Component
public class UpdateVehicleCommandHandler implements CommandHandler<UpdateVehicleCommand, VehicleResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new update vehicle command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka and NATS/JetStream
     * @param registry the command handler registry for auto-registration
     */
    public UpdateVehicleCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public VehicleResponseDto handle(UpdateVehicleCommand command) {
        // Create event from command
        // Only include fields that were provided (nulls for omitted fields support partial updates)
        UpdateVehicleRequested event = new UpdateVehicleRequested(
                command.getUnitId(),
                command.getVehicleType() != null ? command.getVehicleType().name() : null,
                command.getLicensePlate(),
                command.getVin(),
                command.getStatus() != null ? command.getStatus().name() : null,
                command.getLastMaintenanceDate() != null ? command.getLastMaintenanceDate().toString() : null
        );

        // Publish event to Kafka topic "vehicle-events"
        // DualEventPublisher will automatically also publish to NATS/JetStream subject "commands.vehicle.update"
        eventPublisher.publish("vehicle-events", command.getUnitId(), event);

        // Return response DTO
        return new VehicleResponseDto(command.getUnitId());
    }

    @Override
    public Class<UpdateVehicleCommand> getCommandType() {
        return UpdateVehicleCommand.class;
    }
}
