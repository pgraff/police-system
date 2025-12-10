package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.resourceassignment.UnassignResourceRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.ResourceAssignmentResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for UnassignResourceCommand.
 * Creates and publishes an UnassignResourceRequested event to Kafka.
 */
@Component
public class UnassignResourceCommandHandler implements CommandHandler<UnassignResourceCommand, ResourceAssignmentResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new unassign resource command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public UnassignResourceCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public ResourceAssignmentResponseDto handle(UnassignResourceCommand command) {
        // Create event from command
        UnassignResourceRequested event = new UnassignResourceRequested(
                command.getAssignmentId(),
                command.getAssignmentId(),
                command.getResourceId(),
                command.getEndTime()
        );

        // Publish event to Kafka topic "resource-assignment-events"
        eventPublisher.publish("resource-assignment-events", command.getAssignmentId(), event);

        // Return response DTO (using assignmentId as resourceAssignmentId for consistency)
        return new ResourceAssignmentResponseDto(command.getAssignmentId());
    }

    @Override
    public Class<UnassignResourceCommand> getCommandType() {
        return UnassignResourceCommand.class;
    }
}
