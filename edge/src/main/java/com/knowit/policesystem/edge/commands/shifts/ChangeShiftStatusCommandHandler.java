package com.knowit.policesystem.edge.commands.shifts;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.shifts.ChangeShiftStatusRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.ShiftResponseDto;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Command handler for ChangeShiftStatusCommand.
 * Publishes ChangeShiftStatusRequested events to Kafka.
 */
@Component
public class ChangeShiftStatusCommandHandler implements CommandHandler<ChangeShiftStatusCommand, ShiftResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;

    public ChangeShiftStatusCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, TopicConfiguration topicConfiguration) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
        this.topicConfiguration = topicConfiguration;
    }

    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public ShiftResponseDto handle(ChangeShiftStatusCommand command) {
        ChangeShiftStatusRequested event = new ChangeShiftStatusRequested(
                command.getShiftId(),
                EnumConverter.convertStatusToString(command.getStatus())
        );

        eventPublisher.publish(topicConfiguration.SHIFT_EVENTS, command.getShiftId(), event);

        return new ShiftResponseDto(command.getShiftId());
    }

    @Override
    public Class<ChangeShiftStatusCommand> getCommandType() {
        return ChangeShiftStatusCommand.class;
    }
}
