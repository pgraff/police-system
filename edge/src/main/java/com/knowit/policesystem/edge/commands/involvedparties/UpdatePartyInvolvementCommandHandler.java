package com.knowit.policesystem.edge.commands.involvedparties;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.involvedparty.UpdatePartyInvolvementRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.InvolvementResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for UpdatePartyInvolvementCommand.
 * Publishes UpdatePartyInvolvementRequested events to Kafka.
 */
@Component
public class UpdatePartyInvolvementCommandHandler implements CommandHandler<UpdatePartyInvolvementCommand, InvolvementResponseDto> {

    private static final String TOPIC = "involved-party-events";

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new update party involvement command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     */
    public UpdatePartyInvolvementCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public InvolvementResponseDto handle(UpdatePartyInvolvementCommand command) {
        // Create event from command
        UpdatePartyInvolvementRequested event = new UpdatePartyInvolvementRequested(
                command.getInvolvementId(),
                command.getInvolvementId(),
                command.getPartyRoleType() != null ? command.getPartyRoleType().name() : null,
                command.getDescription()
        );

        // Publish event to Kafka topic "involved-party-events"
        eventPublisher.publish(TOPIC, command.getInvolvementId(), event);

        // Return response DTO
        return new InvolvementResponseDto(command.getInvolvementId());
    }

    @Override
    public Class<UpdatePartyInvolvementCommand> getCommandType() {
        return UpdatePartyInvolvementCommand.class;
    }
}
