package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.CallResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ReceiveCallCommand.
 * Creates and publishes a ReceiveCallRequested event to Kafka.
 */
@Component
public class ReceiveCallCommandHandler implements CommandHandler<ReceiveCallCommand, CallResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new receive call command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public ReceiveCallCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public CallResponseDto handle(ReceiveCallCommand command) {
        // Create event from command
        ReceiveCallRequested event = new ReceiveCallRequested(
                command.getCallId(),
                command.getCallNumber(),
                command.getPriority() != null ? command.getPriority().name() : null,
                command.getStatus() != null ? command.getStatus().name() : null,
                command.getReceivedTime(),
                command.getDescription(),
                command.getCallType() != null ? command.getCallType().name() : null
        );

        // Publish event to Kafka topic "call-events"
        eventPublisher.publish("call-events", command.getCallId(), event);

        // Return response DTO
        return new CallResponseDto(command.getCallId(), command.getCallNumber());
    }

    @Override
    public Class<ReceiveCallCommand> getCommandType() {
        return ReceiveCallCommand.class;
    }
}
