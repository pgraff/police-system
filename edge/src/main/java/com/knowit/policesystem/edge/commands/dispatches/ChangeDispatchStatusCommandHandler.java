package com.knowit.policesystem.edge.commands.dispatches;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.dispatches.ChangeDispatchStatusRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.DispatchResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ChangeDispatchStatusCommand.
 * Creates and publishes a ChangeDispatchStatusRequested event to Kafka.
 */
@Component
public class ChangeDispatchStatusCommandHandler implements CommandHandler<ChangeDispatchStatusCommand, DispatchResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new change dispatch status command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public ChangeDispatchStatusCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public DispatchResponseDto handle(ChangeDispatchStatusCommand command) {
        // Convert status enum to string
        String statusString = command.getStatus() != null ? command.getStatus().name() : null;

        // Create event from command
        ChangeDispatchStatusRequested event = new ChangeDispatchStatusRequested(
                command.getDispatchId(),
                statusString
        );

        // Publish event to Kafka topic "dispatch-events"
        eventPublisher.publish("dispatch-events", command.getDispatchId(), event);

        // Return response DTO
        return new DispatchResponseDto(command.getDispatchId());
    }

    @Override
    public Class<ChangeDispatchStatusCommand> getCommandType() {
        return ChangeDispatchStatusCommand.class;
    }
}
