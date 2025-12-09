package com.knowit.policesystem.edge.commands.locations;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.locations.UnlinkLocationFromCallRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.LocationResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for UnlinkLocationFromCallCommand.
 * Creates and publishes an UnlinkLocationFromCallRequested event to Kafka.
 */
@Component
public class UnlinkLocationFromCallCommandHandler implements CommandHandler<UnlinkLocationFromCallCommand, LocationResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new unlink location from call command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public UnlinkLocationFromCallCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
    }

    /** Registers this handler in the command handler registry. */
    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public LocationResponseDto handle(UnlinkLocationFromCallCommand command) {
        // Create event from command
        UnlinkLocationFromCallRequested event = new UnlinkLocationFromCallRequested(
                command.getLocationId(),
                command.getCallId(),
                command.getLocationId()
        );

        // Publish event to Kafka topic "location-events"
        eventPublisher.publish("location-events", command.getLocationId(), event);

        // Return response DTO
        return new LocationResponseDto(command.getLocationId());
    }

    @Override
    public Class<UnlinkLocationFromCallCommand> getCommandType() {
        return UnlinkLocationFromCallCommand.class;
    }
}
