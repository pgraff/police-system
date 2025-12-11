package com.knowit.policesystem.edge.commands.shifts;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.officershifts.CheckOutOfficerRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.OfficerShiftResponseDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for CheckOutOfficerCommand.
 * Creates and publishes a CheckOutOfficerRequested event to Kafka.
 */
@Component
public class CheckOutOfficerCommandHandler implements CommandHandler<CheckOutOfficerCommand, OfficerShiftResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new check-out officer command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public CheckOutOfficerCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public OfficerShiftResponseDto handle(CheckOutOfficerCommand command) {
        // Create event from command
        CheckOutOfficerRequested event = new CheckOutOfficerRequested(
                command.getShiftId(),
                command.getShiftId(),
                command.getBadgeNumber(),
                command.getCheckOutTime()
        );

        // Publish event to Kafka topic
        eventPublisher.publish(topicConfiguration.OFFICER_SHIFT_EVENTS, command.getShiftId(), event);

        // Return response DTO
        return new OfficerShiftResponseDto(command.getShiftId(), command.getBadgeNumber());
    }

    @Override
    public Class<CheckOutOfficerCommand> getCommandType() {
        return CheckOutOfficerCommand.class;
    }
}
