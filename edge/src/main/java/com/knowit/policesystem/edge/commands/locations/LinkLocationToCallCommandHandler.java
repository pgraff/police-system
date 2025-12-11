package com.knowit.policesystem.edge.commands.locations;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.locations.LinkLocationToCallRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.LocationResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
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
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new link location to call command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public LinkLocationToCallCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public LocationResponseDto handle(LinkLocationToCallCommand command) {
        // Create event from command
        LinkLocationToCallRequested event = new LinkLocationToCallRequested(
                command.getLocationId(),
                command.getCallId(),
                command.getLocationId(),
                EnumConverter.convertEnumToString(command.getLocationRoleType()),
                command.getDescription()
        );

        // Publish event to Kafka topic
        // Note: This is NOT a critical event, so it only goes to Kafka (not NATS/JetStream)
        eventPublisher.publish(topicConfiguration.LOCATION_EVENTS, command.getLocationId(), event);

        // Return response DTO
        return new LocationResponseDto(command.getLocationId());
    }

    @Override
    public Class<LinkLocationToCallCommand> getCommandType() {
        return LinkLocationToCallCommand.class;
    }
}
