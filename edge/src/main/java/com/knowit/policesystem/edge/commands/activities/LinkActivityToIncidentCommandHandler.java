package com.knowit.policesystem.edge.commands.activities;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.activities.LinkActivityToIncidentRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.LinkActivityToIncidentResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for LinkActivityToIncidentCommand.
 * Creates and publishes a LinkActivityToIncidentRequested event to Kafka.
 */
@Component
public class LinkActivityToIncidentCommandHandler implements CommandHandler<LinkActivityToIncidentCommand, LinkActivityToIncidentResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new link activity to incident command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public LinkActivityToIncidentCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public LinkActivityToIncidentResponseDto handle(LinkActivityToIncidentCommand command) {
        // Create event from command
        LinkActivityToIncidentRequested event = new LinkActivityToIncidentRequested(
                command.getActivityId(),
                command.getActivityId(),
                command.getIncidentId()
        );

        // Publish event to Kafka topic
        eventPublisher.publish(topicConfiguration.ACTIVITY_EVENTS, command.getActivityId(), event);

        // Return response DTO
        return new LinkActivityToIncidentResponseDto(command.getActivityId(), command.getIncidentId());
    }

    @Override
    public Class<LinkActivityToIncidentCommand> getCommandType() {
        return LinkActivityToIncidentCommand.class;
    }
}
