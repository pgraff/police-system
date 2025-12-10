package com.knowit.policesystem.edge.commands.dispatches;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.dispatches.CreateDispatchRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.DispatchResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for CreateDispatchCommand.
 * Creates and publishes a CreateDispatchRequested event to Kafka.
 */
@Component
public class CreateDispatchCommandHandler implements CommandHandler<CreateDispatchCommand, DispatchResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new create dispatch command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public CreateDispatchCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public DispatchResponseDto handle(CreateDispatchCommand command) {
        // Create event from command
        CreateDispatchRequested event = new CreateDispatchRequested(
                command.getDispatchId(),
                command.getDispatchTime(),
                command.getDispatchType() != null ? command.getDispatchType().name() : null,
                command.getStatus() != null ? command.getStatus().name() : null
        );

        // Publish event to Kafka topic "dispatch-events"
        eventPublisher.publish("dispatch-events", command.getDispatchId(), event);

        // Return response DTO
        return new DispatchResponseDto(command.getDispatchId());
    }

    @Override
    public Class<CreateDispatchCommand> getCommandType() {
        return CreateDispatchCommand.class;
    }
}
