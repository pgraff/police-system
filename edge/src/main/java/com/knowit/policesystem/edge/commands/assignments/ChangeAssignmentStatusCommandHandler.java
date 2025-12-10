package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.assignments.ChangeAssignmentStatusRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.domain.AssignmentStatus;
import com.knowit.policesystem.edge.dto.AssignmentResponseDto;
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

    /**
     * Creates a new change assignment status command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public ChangeAssignmentStatusCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public AssignmentResponseDto handle(ChangeAssignmentStatusCommand command) {
        // Convert status enum to string, handling InProgress -> "In-Progress"
        String statusString = command.getStatus() != null ? convertStatusToString(command.getStatus()) : null;

        // Create event from command
        ChangeAssignmentStatusRequested event = new ChangeAssignmentStatusRequested(
                command.getAssignmentId(),
                statusString
        );

        // Publish event to Kafka topic "assignment-events"
        eventPublisher.publish("assignment-events", command.getAssignmentId(), event);

        // Return response DTO
        return new AssignmentResponseDto(command.getAssignmentId());
    }

    /**
     * Converts AssignmentStatus enum to string format used in API/events.
     * InProgress -> "In-Progress" (with hyphen)
     * Other values remain as-is.
     *
     * @param status the assignment status enum
     * @return the string representation
     */
    private String convertStatusToString(AssignmentStatus status) {
        if (status == AssignmentStatus.InProgress) {
            return "In-Progress";
        }
        return status.name();
    }

    @Override
    public Class<ChangeAssignmentStatusCommand> getCommandType() {
        return ChangeAssignmentStatusCommand.class;
    }
}
