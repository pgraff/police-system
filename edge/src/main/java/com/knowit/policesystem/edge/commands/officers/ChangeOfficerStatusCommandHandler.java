package com.knowit.policesystem.edge.commands.officers;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.officers.ChangeOfficerStatusRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.OfficerStatusResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ChangeOfficerStatusCommand.
 * Creates and publishes a ChangeOfficerStatusRequested event to Kafka and NATS/JetStream.
 */
@Component
public class ChangeOfficerStatusCommandHandler implements CommandHandler<ChangeOfficerStatusCommand, OfficerStatusResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new change officer status command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka and NATS/JetStream
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public ChangeOfficerStatusCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public OfficerStatusResponseDto handle(ChangeOfficerStatusCommand command) {
        // Create event from command
        ChangeOfficerStatusRequested event = new ChangeOfficerStatusRequested(
                command.getBadgeNumber(),
                command.getStatus()
        );

        // Publish event to Kafka topic
        // DualEventPublisher will automatically also publish to NATS/JetStream subject "commands.officer.change-status"
        eventPublisher.publish(topicConfiguration.OFFICER_EVENTS, command.getBadgeNumber(), event);

        // Return response DTO
        return new OfficerStatusResponseDto(command.getBadgeNumber(), command.getStatus());
    }

    @Override
    public Class<ChangeOfficerStatusCommand> getCommandType() {
        return ChangeOfficerStatusCommand.class;
    }
}
