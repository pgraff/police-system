package com.knowit.policesystem.edge.commands.shifts;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.shifts.RecordShiftChangeRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.ShiftChangeResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for RecordShiftChangeCommand.
 * Publishes RecordShiftChangeRequested events to Kafka.
 */
@Component
public class RecordShiftChangeCommandHandler implements CommandHandler<RecordShiftChangeCommand, ShiftChangeResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    public RecordShiftChangeCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
        this.topicConfiguration = topicConfiguration;
    }

    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public ShiftChangeResponseDto handle(RecordShiftChangeCommand command) {
        RecordShiftChangeRequested event = new RecordShiftChangeRequested(
                command.getShiftId(),
                command.getShiftChangeId(),
                command.getChangeTime(),
                EnumConverter.convertEnumToString(command.getChangeType()),
                command.getNotes()
        );

        eventPublisher.publish(topicConfiguration.SHIFT_EVENTS, command.getShiftId(), event);

        return new ShiftChangeResponseDto(command.getShiftChangeId());
    }

    @Override
    public Class<RecordShiftChangeCommand> getCommandType() {
        return RecordShiftChangeCommand.class;
    }
}
