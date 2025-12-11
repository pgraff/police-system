package com.knowit.policesystem.edge.commands.persons;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.persons.RegisterPersonRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.PersonResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for RegisterPersonCommand.
 * Creates and publishes a RegisterPersonRequested event to Kafka and NATS/JetStream.
 */
@Component
public class RegisterPersonCommandHandler implements CommandHandler<RegisterPersonCommand, PersonResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new register person command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka and NATS/JetStream
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public RegisterPersonCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
        this.topicConfiguration = topicConfiguration;
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
    public PersonResponseDto handle(RegisterPersonCommand command) {
        // Create event from command
        RegisterPersonRequested event = new RegisterPersonRequested(
                command.getPersonId(),
                command.getPersonId(),
                command.getFirstName(),
                command.getLastName(),
                command.getDateOfBirth() != null ? command.getDateOfBirth().toString() : null,
                EnumConverter.convertEnumToString(command.getGender()),
                EnumConverter.convertEnumToString(command.getRace()),
                command.getPhoneNumber()
        );

        // Publish event to Kafka topic
        // DualEventPublisher will automatically also publish to NATS/JetStream subject "commands.person.register"
        eventPublisher.publish(topicConfiguration.PERSON_EVENTS, command.getPersonId(), event);

        // Return response DTO
        return new PersonResponseDto(command.getPersonId());
    }

    @Override
    public Class<RegisterPersonCommand> getCommandType() {
        return RegisterPersonCommand.class;
    }
}
