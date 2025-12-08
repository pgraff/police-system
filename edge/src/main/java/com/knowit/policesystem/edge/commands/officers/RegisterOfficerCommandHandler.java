package com.knowit.policesystem.edge.commands.officers;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.officers.RegisterOfficerRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.OfficerResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for RegisterOfficerCommand.
 * Creates and publishes a RegisterOfficerRequested event to Kafka and NATS/JetStream.
 */
@Component
public class RegisterOfficerCommandHandler implements CommandHandler<RegisterOfficerCommand, OfficerResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new register officer command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka and NATS/JetStream
     * @param registry the command handler registry for auto-registration
     */
    public RegisterOfficerCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public OfficerResponseDto handle(RegisterOfficerCommand command) {
        // Create event from command
        RegisterOfficerRequested event = new RegisterOfficerRequested(
                command.getBadgeNumber(),
                command.getFirstName(),
                command.getLastName(),
                command.getRank(),
                command.getEmail(),
                command.getPhoneNumber(),
                command.getHireDate() != null ? command.getHireDate().toString() : null,
                command.getStatus() != null ? command.getStatus().name() : null
        );

        // Publish event to Kafka topic "officer-events"
        // DualEventPublisher will automatically also publish to NATS/JetStream subject "commands.officer.register"
        eventPublisher.publish("officer-events", command.getBadgeNumber(), event);

        // Return response DTO
        return new OfficerResponseDto(command.getBadgeNumber());
    }

    @Override
    public Class<RegisterOfficerCommand> getCommandType() {
        return RegisterOfficerCommand.class;
    }
}
