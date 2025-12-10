package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.resourceassignment.AssignResourceRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.ResourceAssignmentResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Command handler for AssignResourceCommand.
 * Creates and publishes an AssignResourceRequested event to Kafka.
 */
@Component
public class AssignResourceCommandHandler implements CommandHandler<AssignResourceCommand, ResourceAssignmentResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new assign resource command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public AssignResourceCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
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
    public ResourceAssignmentResponseDto handle(AssignResourceCommand command) {
        // Generate resource assignment ID
        String resourceAssignmentId = UUID.randomUUID().toString();

        // Create event from command
        AssignResourceRequested event = new AssignResourceRequested(
                command.getAssignmentId(),
                command.getAssignmentId(),
                command.getResourceId(),
                command.getResourceType() != null ? command.getResourceType().name() : null,
                command.getRoleType() != null ? command.getRoleType().name() : null,
                command.getStatus(),
                command.getStartTime()
        );

        // Publish event to Kafka topic "resource-assignment-events"
        eventPublisher.publish("resource-assignment-events", command.getAssignmentId(), event);

        // Return response DTO
        return new ResourceAssignmentResponseDto(resourceAssignmentId);
    }

    @Override
    public Class<AssignResourceCommand> getCommandType() {
        return AssignResourceCommand.class;
    }
}
