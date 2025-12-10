package com.knowit.policesystem.edge.commands.involvedparties;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.involvedparty.InvolvePartyRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.InvolvementResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for InvolvePartyCommand.
 * Creates and publishes an InvolvePartyRequested event to Kafka.
 */
@Component
public class InvolvePartyCommandHandler implements CommandHandler<InvolvePartyCommand, InvolvementResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new involve party command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public InvolvePartyCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public InvolvementResponseDto handle(InvolvePartyCommand command) {
        // Create event from command
        InvolvePartyRequested event = new InvolvePartyRequested(
                command.getInvolvementId(),
                command.getInvolvementId(),
                command.getPersonId(),
                command.getIncidentId(),
                command.getCallId(),
                command.getActivityId(),
                command.getPartyRoleType() != null ? command.getPartyRoleType().name() : null,
                command.getDescription(),
                command.getInvolvementStartTime()
        );

        // Publish event to Kafka topic "involved-party-events"
        eventPublisher.publish("involved-party-events", command.getInvolvementId(), event);

        // Return response DTO
        return new InvolvementResponseDto(command.getInvolvementId());
    }

    @Override
    public Class<InvolvePartyCommand> getCommandType() {
        return InvolvePartyCommand.class;
    }
}
