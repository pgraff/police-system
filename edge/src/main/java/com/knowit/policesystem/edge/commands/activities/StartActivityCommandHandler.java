package com.knowit.policesystem.edge.commands.activities;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.activities.StartActivityRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.ActivityResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for StartActivityCommand.
 * Creates and publishes a StartActivityRequested event to Kafka.
 */
@Component
public class StartActivityCommandHandler implements CommandHandler<StartActivityCommand, ActivityResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new start activity command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public StartActivityCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public ActivityResponseDto handle(StartActivityCommand command) {
        // Create event from command
        StartActivityRequested event = new StartActivityRequested(
                command.getActivityId(),
                command.getActivityTime(),
                EnumConverter.convertEnumToString(command.getActivityType()),
                command.getDescription(),
                EnumConverter.convertStatusToString(command.getStatus())
        );

        // Publish event to Kafka topic
        eventPublisher.publish(topicConfiguration.ACTIVITY_EVENTS, command.getActivityId(), event);

        // Return response DTO
        return new ActivityResponseDto(command.getActivityId());
    }

    @Override
    public Class<StartActivityCommand> getCommandType() {
        return StartActivityCommand.class;
    }
}
