package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.assignments.CompleteAssignmentRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.AssignmentResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for CompleteAssignmentCommand.
 * Creates and publishes a CompleteAssignmentRequested event to Kafka.
 */
@Component
public class CompleteAssignmentCommandHandler implements CommandHandler<CompleteAssignmentCommand, AssignmentResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new complete assignment command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public CompleteAssignmentCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public AssignmentResponseDto handle(CompleteAssignmentCommand command) {
        CompleteAssignmentRequested event = new CompleteAssignmentRequested(
                command.getAssignmentId(),
                command.getCompletedTime()
        );

        eventPublisher.publish("assignment-events", command.getAssignmentId(), event);

        return new AssignmentResponseDto(command.getAssignmentId());
    }

    @Override
    public Class<CompleteAssignmentCommand> getCommandType() {
        return CompleteAssignmentCommand.class;
    }
}
