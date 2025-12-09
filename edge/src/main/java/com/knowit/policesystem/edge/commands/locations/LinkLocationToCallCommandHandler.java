package com.knowit.policesystem.edge.commands.locations;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.locations.LinkLocationToCallRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.LocationResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for LinkLocationToCallCommand.
 * Creates and publishes a LinkLocationToCallRequested event to Kafka.
 */
@Component
public class LinkLocationToCallCommandHandler implements CommandHandler<LinkLocationToCallCommand, LocationResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new link location to call command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public LinkLocationToCallCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public LocationResponseDto handle(LinkLocationToCallCommand command) {
        // Create event from command
        LinkLocationToCallRequested event = new LinkLocationToCallRequested(
                command.getLocationId(),
                command.getCallId(),
                command.getLocationId(),
                command.getLocationRoleType() != null ? command.getLocationRoleType().name() : null,
                command.getDescription()
        );

        // Publish event to Kafka topic "location-events"
        // Note: This is NOT a critical event, so it only goes to Kafka (not NATS/JetStream)
        eventPublisher.publish("location-events", command.getLocationId(), event);

        // Return response DTO
        return new LocationResponseDto(command.getLocationId());
    }

    @Override
    public Class<LinkLocationToCallCommand> getCommandType() {
        return LinkLocationToCallCommand.class;
    }
}
