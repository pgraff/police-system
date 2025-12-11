package com.knowit.policesystem.edge.commands.activities;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.activities.UpdateActivityRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.ActivityResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for UpdateActivityCommand.
 * Publishes UpdateActivityRequested events to Kafka.
 */
@Component
public class UpdateActivityCommandHandler implements CommandHandler<UpdateActivityCommand, ActivityResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    public UpdateActivityCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
        this.topicConfiguration = topicConfiguration;
    }

    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public ActivityResponseDto handle(UpdateActivityCommand command) {
        UpdateActivityRequested event = new UpdateActivityRequested(
                command.getActivityId(),
                command.getDescription()
        );

        eventPublisher.publish(topicConfiguration.ACTIVITY_EVENTS, command.getActivityId(), event);

        return new ActivityResponseDto(command.getActivityId());
    }

    @Override
    public Class<UpdateActivityCommand> getCommandType() {
        return UpdateActivityCommand.class;
    }
}
