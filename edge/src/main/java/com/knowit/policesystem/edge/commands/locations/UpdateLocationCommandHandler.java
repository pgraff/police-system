package com.knowit.policesystem.edge.commands.locations;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.locations.UpdateLocationRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.LocationResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for UpdateLocationCommand.
 * Creates and publishes an UpdateLocationRequested event to Kafka and NATS/JetStream.
 */
@Component
public class UpdateLocationCommandHandler implements CommandHandler<UpdateLocationCommand, LocationResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new update location command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka and NATS/JetStream
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public UpdateLocationCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public LocationResponseDto handle(UpdateLocationCommand command) {
        // Create event from command
        // Only include fields that were provided (nulls for omitted fields support partial updates)
        UpdateLocationRequested event = new UpdateLocationRequested(
                command.getLocationId(),
                command.getAddress(),
                command.getCity(),
                command.getState(),
                command.getZipCode(),
                command.getLatitude() != null ? command.getLatitude().toString() : null,
                command.getLongitude() != null ? command.getLongitude().toString() : null,
                EnumConverter.convertEnumToString(command.getLocationType())
        );

        // Publish event to Kafka topic
        // DualEventPublisher will automatically also publish to NATS/JetStream subject "commands.location.update"
        eventPublisher.publish(topicConfiguration.LOCATION_EVENTS, command.getLocationId(), event);

        // Return response DTO
        return new LocationResponseDto(command.getLocationId());
    }

    @Override
    public Class<UpdateLocationCommand> getCommandType() {
        return UpdateLocationCommand.class;
    }
}
