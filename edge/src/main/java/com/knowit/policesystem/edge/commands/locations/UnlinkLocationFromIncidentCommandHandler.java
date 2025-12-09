package com.knowit.policesystem.edge.commands.locations;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.locations.UnlinkLocationFromIncidentRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.LocationResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for UnlinkLocationFromIncidentCommand.
 * Creates and publishes an UnlinkLocationFromIncidentRequested event to Kafka.
 */
@Component
public class UnlinkLocationFromIncidentCommandHandler implements CommandHandler<UnlinkLocationFromIncidentCommand, LocationResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new unlink location from incident command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public UnlinkLocationFromIncidentCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public LocationResponseDto handle(UnlinkLocationFromIncidentCommand command) {
        // Create event from command
        UnlinkLocationFromIncidentRequested event = new UnlinkLocationFromIncidentRequested(
                command.getLocationId(),
                command.getIncidentId(),
                command.getLocationId()
        );

        // Publish event to Kafka topic "location-events"
        // Note: This is NOT a critical event, so it only goes to Kafka (not NATS/JetStream)
        eventPublisher.publish("location-events", command.getLocationId(), event);

        // Return response DTO
        return new LocationResponseDto(command.getLocationId());
    }

    @Override
    public Class<UnlinkLocationFromIncidentCommand> getCommandType() {
        return UnlinkLocationFromIncidentCommand.class;
    }
}
