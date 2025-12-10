package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.resourceassignment.ChangeResourceAssignmentStatusRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.domain.ResourceAssignmentStatus;
import com.knowit.policesystem.edge.dto.ResourceAssignmentResponseDto;
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

    /**
     * Creates a new change resource assignment status command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public ChangeResourceAssignmentStatusCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public ResourceAssignmentResponseDto handle(ChangeResourceAssignmentStatusCommand command) {
        // Convert status enum to string, handling InProgress -> "In-Progress"
        String statusString = command.getStatus() != null ? convertStatusToString(command.getStatus()) : null;

        // Create event from command
        ChangeResourceAssignmentStatusRequested event = new ChangeResourceAssignmentStatusRequested(
                command.getAssignmentId(),
                command.getAssignmentId(),
                command.getResourceId(),
                statusString
        );

        // Publish event to Kafka topic "resource-assignment-events"
        eventPublisher.publish("resource-assignment-events", command.getAssignmentId(), event);

        // Return response DTO (using assignmentId as resourceAssignmentId for consistency)
        return new ResourceAssignmentResponseDto(command.getAssignmentId());
    }

    /**
     * Converts ResourceAssignmentStatus enum to string format used in API/events.
     * InProgress -> "In-Progress" (with hyphen)
     * Other values remain as-is.
     *
     * @param status the resource assignment status enum
     * @return the string representation
     */
    private String convertStatusToString(ResourceAssignmentStatus status) {
        if (status == ResourceAssignmentStatus.InProgress) {
            return "In-Progress";
        }
        return status.name();
    }

    @Override
    public Class<ChangeResourceAssignmentStatusCommand> getCommandType() {
        return ChangeResourceAssignmentStatusCommand.class;
    }
}
