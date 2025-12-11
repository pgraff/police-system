package com.knowit.policesystem.edge.commands.persons;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.persons.UpdatePersonRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.PersonResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
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
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new update person command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka and NATS/JetStream
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public UpdatePersonCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public PersonResponseDto handle(UpdatePersonCommand command) {
        // Create event from command
        // Only include fields that were provided (nulls for omitted fields support partial updates)
        UpdatePersonRequested event = new UpdatePersonRequested(
                command.getPersonId(),
                command.getFirstName(),
                command.getLastName(),
                command.getDateOfBirth() != null ? command.getDateOfBirth().toString() : null,
                EnumConverter.convertEnumToString(command.getGender()),
                EnumConverter.convertEnumToString(command.getRace()),
                command.getPhoneNumber()
        );

        // Publish event to Kafka topic
        // DualEventPublisher will automatically also publish to NATS/JetStream subject "commands.person.update"
        eventPublisher.publish(topicConfiguration.PERSON_EVENTS, command.getPersonId(), event);

        // Return response DTO
        return new PersonResponseDto(command.getPersonId());
    }

    @Override
    public Class<UpdatePersonCommand> getCommandType() {
        return UpdatePersonCommand.class;
    }
}
