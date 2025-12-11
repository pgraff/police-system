package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.resourceassignment.ChangeResourceAssignmentStatusRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.ResourceAssignmentResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ChangeResourceAssignmentStatusCommand.
 * Creates and publishes a ChangeResourceAssignmentStatusRequested event to Kafka.
 */
@Component
public class ChangeResourceAssignmentStatusCommandHandler implements CommandHandler<ChangeResourceAssignmentStatusCommand, ResourceAssignmentResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new change resource assignment status command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public ChangeResourceAssignmentStatusCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public ResourceAssignmentResponseDto handle(ChangeResourceAssignmentStatusCommand command) {
        // Create event from command
        ChangeResourceAssignmentStatusRequested event = new ChangeResourceAssignmentStatusRequested(
                command.getAssignmentId(),
                command.getAssignmentId(),
                command.getResourceId(),
                EnumConverter.convertStatusToString(command.getStatus())
        );

        // Publish event to Kafka topic
        eventPublisher.publish(topicConfiguration.RESOURCE_ASSIGNMENT_EVENTS, command.getAssignmentId(), event);

        // Return response DTO (using assignmentId as resourceAssignmentId for consistency)
        return new ResourceAssignmentResponseDto(command.getAssignmentId());
    }

    @Override
    public Class<ChangeResourceAssignmentStatusCommand> getCommandType() {
        return ChangeResourceAssignmentStatusCommand.class;
    }
}
