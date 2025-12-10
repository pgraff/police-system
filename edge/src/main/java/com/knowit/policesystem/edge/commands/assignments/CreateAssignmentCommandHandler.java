package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.assignments.CreateAssignmentRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.domain.AssignmentStatus;
import com.knowit.policesystem.edge.dto.AssignmentResponseDto;
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

    /**
     * Creates a new create assignment command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public CreateAssignmentCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public AssignmentResponseDto handle(CreateAssignmentCommand command) {
        // Convert status enum to string, handling InProgress -> "In-Progress"
        String statusString = command.getStatus() != null ? convertStatusToString(command.getStatus()) : null;

        // Create event from command
        CreateAssignmentRequested event = new CreateAssignmentRequested(
                command.getAssignmentId(),
                command.getAssignmentId(),
                command.getAssignedTime(),
                command.getAssignmentType() != null ? command.getAssignmentType().name() : null,
                statusString,
                command.getIncidentId(),
                command.getCallId()
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
    public Class<CreateAssignmentCommand> getCommandType() {
        return CreateAssignmentCommand.class;
    }
}
