package com.knowit.policesystem.edge.commands.incidents;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.incidents.ClearIncidentRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.IncidentResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ClearIncidentCommand.
 * Publishes ClearIncidentRequested via dual publisher (Kafka + NATS for critical events).
 */
@Component
public class ClearIncidentCommandHandler implements CommandHandler<ClearIncidentCommand, IncidentResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    public ClearIncidentCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
    }

    /** Registers this handler in the command handler registry. */
    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public IncidentResponseDto handle(ClearIncidentCommand command) {
        ClearIncidentRequested event = new ClearIncidentRequested(
                command.getIncidentId(),
                command.getClearedTime()
        );

        // Publish to incident-events; DualEventPublisher will also publish to NATS for critical events.
        eventPublisher.publish("incident-events", command.getIncidentId(), event);

        return new IncidentResponseDto(command.getIncidentId(), null);
    }

    @Override
    public Class<ClearIncidentCommand> getCommandType() {
        return ClearIncidentCommand.class;
    }
}
