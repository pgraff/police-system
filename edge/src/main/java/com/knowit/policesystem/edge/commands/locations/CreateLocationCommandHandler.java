package com.knowit.policesystem.edge.commands.locations;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.locations.CreateLocationRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.LocationResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for CreateLocationCommand.
 * Creates and publishes a CreateLocationRequested event to Kafka and NATS/JetStream.
 */
@Component
public class CreateLocationCommandHandler implements CommandHandler<CreateLocationCommand, LocationResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new create location command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka and NATS/JetStream
     * @param registry the command handler registry for auto-registration
     */
    public CreateLocationCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public LocationResponseDto handle(CreateLocationCommand command) {
        // Create event from command
        CreateLocationRequested event = new CreateLocationRequested(
                command.getLocationId(),
                command.getLocationId(),
                command.getAddress(),
                command.getCity(),
                command.getState(),
                command.getZipCode(),
                command.getLatitude() != null ? command.getLatitude().toString() : null,
                command.getLongitude() != null ? command.getLongitude().toString() : null,
                command.getLocationType() != null ? command.getLocationType().name() : null
        );

        // Publish event to Kafka topic "location-events"
        // DualEventPublisher will automatically also publish to NATS/JetStream subject "commands.location.create"
        eventPublisher.publish("location-events", command.getLocationId(), event);

        // Return response DTO
        return new LocationResponseDto(command.getLocationId());
    }

    @Override
    public Class<CreateLocationCommand> getCommandType() {
        return CreateLocationCommand.class;
    }
}
