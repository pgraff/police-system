package com.knowit.policesystem.edge.commands.assignments;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.assignments.LinkAssignmentToDispatchRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.LinkAssignmentToDispatchResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for LinkAssignmentToDispatchCommand.
 * Creates and publishes a LinkAssignmentToDispatchRequested event to Kafka.
 */
@Component
public class LinkAssignmentToDispatchCommandHandler implements CommandHandler<LinkAssignmentToDispatchCommand, LinkAssignmentToDispatchResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new link assignment to dispatch command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public LinkAssignmentToDispatchCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public LinkAssignmentToDispatchResponseDto handle(LinkAssignmentToDispatchCommand command) {
        // Create event from command
        LinkAssignmentToDispatchRequested event = new LinkAssignmentToDispatchRequested(
                command.getAssignmentId(),
                command.getAssignmentId(),
                command.getDispatchId()
        );

        // Publish event to Kafka topic "assignment-events"
        // Note: This is NOT a critical event, so it only goes to Kafka (not NATS/JetStream)
        eventPublisher.publish("assignment-events", command.getAssignmentId(), event);

        // Return response DTO
        return new LinkAssignmentToDispatchResponseDto(command.getAssignmentId(), command.getDispatchId());
    }

    @Override
    public Class<LinkAssignmentToDispatchCommand> getCommandType() {
        return LinkAssignmentToDispatchCommand.class;
    }
}
