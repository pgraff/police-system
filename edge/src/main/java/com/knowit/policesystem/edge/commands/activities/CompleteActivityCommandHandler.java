package com.knowit.policesystem.edge.commands.activities;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.activities.CompleteActivityRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.ActivityResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for CompleteActivityCommand.
 * Creates and publishes a CompleteActivityRequested event to Kafka.
 */
@Component
public class CompleteActivityCommandHandler implements CommandHandler<CompleteActivityCommand, ActivityResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new complete activity command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public CompleteActivityCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public ActivityResponseDto handle(CompleteActivityCommand command) {
        CompleteActivityRequested event = new CompleteActivityRequested(
                command.getActivityId(),
                command.getCompletedTime()
        );

        eventPublisher.publish(topicConfiguration.ACTIVITY_EVENTS, command.getActivityId(), event);

        return new ActivityResponseDto(command.getActivityId());
    }

    @Override
    public Class<CompleteActivityCommand> getCommandType() {
        return CompleteActivityCommand.class;
    }
}
