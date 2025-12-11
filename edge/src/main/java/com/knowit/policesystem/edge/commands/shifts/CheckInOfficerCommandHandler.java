package com.knowit.policesystem.edge.commands.shifts;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.officershifts.CheckInOfficerRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.OfficerShiftResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for CheckInOfficerCommand.
 * Creates and publishes a CheckInOfficerRequested event to Kafka.
 */
@Component
public class CheckInOfficerCommandHandler implements CommandHandler<CheckInOfficerCommand, OfficerShiftResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new check-in officer command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public CheckInOfficerCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public OfficerShiftResponseDto handle(CheckInOfficerCommand command) {
        // Create event from command
        CheckInOfficerRequested event = new CheckInOfficerRequested(
                command.getShiftId(),
                command.getShiftId(),
                command.getBadgeNumber(),
                command.getCheckInTime(),
                EnumConverter.convertEnumToString(command.getShiftRoleType())
        );

        // Publish event to Kafka topic
        eventPublisher.publish(topicConfiguration.OFFICER_SHIFT_EVENTS, command.getShiftId(), event);

        // Return response DTO
        return new OfficerShiftResponseDto(command.getShiftId(), command.getBadgeNumber());
    }

    @Override
    public Class<CheckInOfficerCommand> getCommandType() {
        return CheckInOfficerCommand.class;
    }
}
