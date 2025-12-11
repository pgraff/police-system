package com.knowit.policesystem.edge.commands.shifts;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.shifts.StartShiftRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.ShiftResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for StartShiftCommand.
 * Creates and publishes a StartShiftRequested event to Kafka.
 */
@Component
public class StartShiftCommandHandler implements CommandHandler<StartShiftCommand, ShiftResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    /**
     * Creates a new start shift command handler.
     *
     * @param eventPublisher the event publisher for publishing events to Kafka
     * @param registry the command handler registry for auto-registration
     * @param topicConfiguration the topic configuration for Kafka topics
     */
    public StartShiftCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
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
    public ShiftResponseDto handle(StartShiftCommand command) {
        StartShiftRequested event = new StartShiftRequested(
                command.getShiftId(),
                command.getStartTime(),
                command.getEndTime(),
                EnumConverter.convertEnumToString(command.getShiftType()),
                EnumConverter.convertStatusToString(command.getStatus())
        );

        eventPublisher.publish(topicConfiguration.SHIFT_EVENTS, command.getShiftId(), event);

        return new ShiftResponseDto(command.getShiftId());
    }

    @Override
    public Class<StartShiftCommand> getCommandType() {
        return StartShiftCommand.class;
    }
}
