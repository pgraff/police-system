package com.knowit.policesystem.edge.commands.vehicles;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.vehicles.RegisterVehicleRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.VehicleResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for RegisterVehicleCommand.
 * Creates and publishes a RegisterVehicleRequested event to Kafka and NATS/JetStream.
 */
@Component
public class RegisterVehicleCommandHandler implements CommandHandler<RegisterVehicleCommand, VehicleResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new register vehicle command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka and NATS/JetStream
     * @param registry the command handler registry for auto-registration
     */
    public RegisterVehicleCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public VehicleResponseDto handle(RegisterVehicleCommand command) {
        // Create event from command
        RegisterVehicleRequested event = new RegisterVehicleRequested(
                command.getUnitId(),
                command.getVehicleType() != null ? command.getVehicleType().name() : null,
                command.getLicensePlate(),
                command.getVin(),
                command.getStatus() != null ? command.getStatus().name() : null,
                command.getLastMaintenanceDate() != null ? command.getLastMaintenanceDate().toString() : null
        );

        // Publish event to Kafka topic "vehicle-events"
        // DualEventPublisher will automatically also publish to NATS/JetStream subject "commands.vehicle.register"
        eventPublisher.publish("vehicle-events", command.getUnitId(), event);

        // Return response DTO
        return new VehicleResponseDto(command.getUnitId());
    }

    @Override
    public Class<RegisterVehicleCommand> getCommandType() {
        return RegisterVehicleCommand.class;
    }
}
