package com.knowit.policesystem.edge.commands.incidents;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.incidents.DispatchIncidentRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.IncidentResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for DispatchIncidentCommand.
 * Publishes DispatchIncidentRequested events using the dual publisher (Kafka + NATS for critical events).
 */
@Component
public class DispatchIncidentCommandHandler implements CommandHandler<DispatchIncidentCommand, IncidentResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    public DispatchIncidentCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
    }

    /**
     * Registers this handler in the command handler registry.
     */
    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public IncidentResponseDto handle(DispatchIncidentCommand command) {
        DispatchIncidentRequested event = new DispatchIncidentRequested(
                command.getIncidentId(),
                command.getDispatchedTime()
        );

        // Publish event to Kafka topic "incident-events" (DualEventPublisher will also publish to NATS for critical events)
        eventPublisher.publish("incident-events", command.getIncidentId(), event);

        return new IncidentResponseDto(command.getIncidentId(), null);
    }

    @Override
    public Class<DispatchIncidentCommand> getCommandType() {
        return DispatchIncidentCommand.class;
    }
}
