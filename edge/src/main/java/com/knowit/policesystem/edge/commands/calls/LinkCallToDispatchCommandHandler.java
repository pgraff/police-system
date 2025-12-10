package com.knowit.policesystem.edge.commands.calls;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.calls.LinkCallToDispatchRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.LinkCallToDispatchResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for LinkCallToDispatchCommand.
 * Creates and publishes a LinkCallToDispatchRequested event to Kafka.
 */
@Component
public class LinkCallToDispatchCommandHandler implements CommandHandler<LinkCallToDispatchCommand, LinkCallToDispatchResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new link call to dispatch command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public LinkCallToDispatchCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public LinkCallToDispatchResponseDto handle(LinkCallToDispatchCommand command) {
        // Create event from command
        LinkCallToDispatchRequested event = new LinkCallToDispatchRequested(
                command.getCallId(),
                command.getCallId(),
                command.getDispatchId()
        );

        // Publish event to Kafka topic "call-events"
        // Note: This is NOT a critical event, so it only goes to Kafka (not NATS/JetStream)
        eventPublisher.publish("call-events", command.getCallId(), event);

        // Return response DTO
        return new LinkCallToDispatchResponseDto(command.getCallId(), command.getDispatchId());
    }

    @Override
    public Class<LinkCallToDispatchCommand> getCommandType() {
        return LinkCallToDispatchCommand.class;
    }
}
