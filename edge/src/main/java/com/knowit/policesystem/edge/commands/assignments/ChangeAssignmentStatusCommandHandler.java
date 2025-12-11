package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.assignments.ChangeAssignmentStatusRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.AssignmentResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ChangeAssignmentStatusCommand.
 * Creates and publishes a ChangeAssignmentStatusRequested event to Kafka.
 */
@Component
public class ChangeAssignmentStatusCommandHandler implements CommandHandler<ChangeAssignmentStatusCommand, AssignmentResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new change assignment status command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public ChangeAssignmentStatusCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public AssignmentResponseDto handle(ChangeAssignmentStatusCommand command) {
        // Create event from command
        ChangeAssignmentStatusRequested event = new ChangeAssignmentStatusRequested(
                command.getAssignmentId(),
                EnumConverter.convertStatusToString(command.getStatus())
        );

        // Publish event to Kafka topic
        eventPublisher.publish(topicConfiguration.ASSIGNMENT_EVENTS, command.getAssignmentId(), event);

        // Return response DTO
        return new AssignmentResponseDto(command.getAssignmentId());
    }

    @Override
    public Class<ChangeAssignmentStatusCommand> getCommandType() {
        return ChangeAssignmentStatusCommand.class;
    }
}
