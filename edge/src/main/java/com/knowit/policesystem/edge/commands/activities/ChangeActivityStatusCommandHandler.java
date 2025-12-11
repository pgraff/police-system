package com.knowit.policesystem.edge.commands.activities;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.activities.ChangeActivityStatusRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.ActivityResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ChangeActivityStatusCommand.
 * Creates and publishes a ChangeActivityStatusRequested event to Kafka.
 */
@Component
public class ChangeActivityStatusCommandHandler implements CommandHandler<ChangeActivityStatusCommand, ActivityResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new change activity status command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public ChangeActivityStatusCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public ActivityResponseDto handle(ChangeActivityStatusCommand command) {
        // Create event from command
        ChangeActivityStatusRequested event = new ChangeActivityStatusRequested(
                command.getActivityId(),
                EnumConverter.convertStatusToString(command.getStatus())
        );

        // Publish event to Kafka topic
        eventPublisher.publish(topicConfiguration.ACTIVITY_EVENTS, command.getActivityId(), event);

        // Return response DTO
        return new ActivityResponseDto(command.getActivityId());
    }

    @Override
    public Class<ChangeActivityStatusCommand> getCommandType() {
        return ChangeActivityStatusCommand.class;
    }
}
