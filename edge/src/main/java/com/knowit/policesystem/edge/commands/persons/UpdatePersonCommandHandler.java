package com.knowit.policesystem.edge.commands.persons;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.persons.UpdatePersonRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.dto.PersonResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for UpdatePersonCommand.
 * Creates and publishes an UpdatePersonRequested event to Kafka and NATS/JetStream.
 */
@Component
public class UpdatePersonCommandHandler implements CommandHandler<UpdatePersonCommand, PersonResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;

    /**
     * Creates a new update person command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka and NATS/JetStream
     * @param registry the command handler registry for auto-registration
     */
    public UpdatePersonCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry) {
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
    public PersonResponseDto handle(UpdatePersonCommand command) {
        // Create event from command
        // Only include fields that were provided (nulls for omitted fields support partial updates)
        UpdatePersonRequested event = new UpdatePersonRequested(
                command.getPersonId(),
                command.getFirstName(),
                command.getLastName(),
                command.getDateOfBirth() != null ? command.getDateOfBirth().toString() : null,
                command.getGender() != null ? command.getGender().name() : null,
                command.getRace() != null ? command.getRace().name() : null,
                command.getPhoneNumber()
        );

        // Publish event to Kafka topic "person-events"
        // DualEventPublisher will automatically also publish to NATS/JetStream subject "commands.person.update"
        eventPublisher.publish("person-events", command.getPersonId(), event);

        // Return response DTO
        return new PersonResponseDto(command.getPersonId());
    }

    @Override
    public Class<UpdatePersonCommand> getCommandType() {
        return UpdatePersonCommand.class;
    }
}
