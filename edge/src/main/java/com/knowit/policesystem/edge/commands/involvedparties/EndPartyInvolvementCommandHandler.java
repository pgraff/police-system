package com.knowit.policesystem.edge.commands.involvedparties;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.involvedparty.EndPartyInvolvementRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.InvolvementResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for EndPartyInvolvementCommand.
 * Publishes EndPartyInvolvementRequested events to Kafka.
 */
@Component
public class EndPartyInvolvementCommandHandler implements CommandHandler<EndPartyInvolvementCommand, InvolvementResponseDto> {

    private static final String TOPIC = "involved-party-events";

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new end party involvement command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public EndPartyInvolvementCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public InvolvementResponseDto handle(EndPartyInvolvementCommand command) {
        // Create event from command
        EndPartyInvolvementRequested event = new EndPartyInvolvementRequested(
                command.getInvolvementId(),
                command.getInvolvementId(),
                command.getInvolvementEndTime()
        );

        // Publish event to Kafka topic "involved-party-events"
        eventPublisher.publish(TOPIC, command.getInvolvementId(), event);

        // Return response DTO
        return new InvolvementResponseDto(command.getInvolvementId());
    }

    @Override
    public Class<EndPartyInvolvementCommand> getCommandType() {
        return EndPartyInvolvementCommand.class;
    }
}
