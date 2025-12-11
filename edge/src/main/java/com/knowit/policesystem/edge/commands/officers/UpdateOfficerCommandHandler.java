package com.knowit.policesystem.edge.commands.officers;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.officers.UpdateOfficerRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.OfficerResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for UpdateOfficerCommand.
 * Creates and publishes an UpdateOfficerRequested event to Kafka and NATS/JetStream.
 */
@Component
public class UpdateOfficerCommandHandler implements CommandHandler<UpdateOfficerCommand, OfficerResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new update officer command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka and NATS/JetStream
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public UpdateOfficerCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public OfficerResponseDto handle(UpdateOfficerCommand command) {
        // Create event from command
        // Only include fields that were provided (nulls for omitted fields support partial updates)
        UpdateOfficerRequested event = new UpdateOfficerRequested(
                command.getBadgeNumber(),
                command.getFirstName(),
                command.getLastName(),
                command.getRank(),
                command.getEmail(),
                command.getPhoneNumber(),
                command.getHireDate() != null ? command.getHireDate().toString() : null
        );

        // Publish event to Kafka topic
        // DualEventPublisher will automatically also publish to NATS/JetStream subject "commands.officer.update"
        eventPublisher.publish(topicConfiguration.OFFICER_EVENTS, command.getBadgeNumber(), event);

        // Return response DTO
        return new OfficerResponseDto(command.getBadgeNumber());
    }

    @Override
    public Class<UpdateOfficerCommand> getCommandType() {
        return UpdateOfficerCommand.class;
    }
}
