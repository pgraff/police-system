package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.assignments.CreateAssignmentRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.AssignmentResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for CreateAssignmentCommand.
 * Creates and publishes a CreateAssignmentRequested event to Kafka.
 */
@Component
public class CreateAssignmentCommandHandler implements CommandHandler<CreateAssignmentCommand, AssignmentResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new create assignment command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public CreateAssignmentCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public AssignmentResponseDto handle(CreateAssignmentCommand command) {
        // Create event from command
        CreateAssignmentRequested event = new CreateAssignmentRequested(
                command.getAssignmentId(),
                command.getAssignmentId(),
                command.getAssignedTime(),
                EnumConverter.convertEnumToString(command.getAssignmentType()),
                EnumConverter.convertStatusToString(command.getStatus()),
                command.getIncidentId(),
                command.getCallId()
        );

        // Publish event to Kafka topic
        eventPublisher.publish(topicConfiguration.ASSIGNMENT_EVENTS, command.getAssignmentId(), event);

        // Return response DTO
        return new AssignmentResponseDto(command.getAssignmentId());
    }

    @Override
    public Class<CreateAssignmentCommand> getCommandType() {
        return CreateAssignmentCommand.class;
    }
}
