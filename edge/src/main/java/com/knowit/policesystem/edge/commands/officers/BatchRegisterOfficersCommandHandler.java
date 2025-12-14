package com.knowit.policesystem.edge.commands.officers;

import com.knowit.policesystem.common.events.EventPublisher;
import com.knowit.policesystem.common.events.officers.RegisterOfficerRequested;
import com.knowit.policesystem.edge.commands.CommandHandler;
import com.knowit.policesystem.edge.commands.CommandHandlerRegistry;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.dto.BatchCreateResponseDto;
import com.knowit.policesystem.edge.dto.RegisterOfficerRequestDto;
import com.knowit.policesystem.edge.services.officers.OfficerConflictService;
import com.knowit.policesystem.edge.util.EnumConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Command handler for BatchRegisterOfficersCommand.
 * Processes multiple officer registrations, collecting successes and failures.
 * Implements idempotency by skipping duplicates based on badge number.
 */
@Component
public class BatchRegisterOfficersCommandHandler implements CommandHandler<BatchRegisterOfficersCommand, BatchCreateResponseDto> {

    private final EventPublisher eventPublisher;
    private final CommandHandlerRegistry registry;
    private final TopicConfiguration topicConfiguration;
    private final OfficerConflictService officerConflictService;

    public BatchRegisterOfficersCommandHandler(EventPublisher eventPublisher, CommandHandlerRegistry registry, 
                                               TopicConfiguration topicConfiguration, OfficerConflictService officerConflictService) {
        this.eventPublisher = eventPublisher;
        this.registry = registry;
        this.topicConfiguration = topicConfiguration;
        this.officerConflictService = officerConflictService;
    }

    /**
     * Registers this handler in the command handler registry.
     */
    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public BatchCreateResponseDto handle(BatchRegisterOfficersCommand command) {
        var requestDto = command.getRequestDto();
        List<String> created = new ArrayList<>();
        List<BatchCreateResponseDto.BatchFailure> failed = new ArrayList<>();
        Set<String> seenBadgeNumbers = new HashSet<>();

        for (RegisterOfficerRequestDto officerDto : requestDto.getOfficers()) {
            String badgeNumber = officerDto.getBadgeNumber();
            
            // Check for duplicates within the batch
            if (seenBadgeNumbers.contains(badgeNumber)) {
                failed.add(new BatchCreateResponseDto.BatchFailure(
                        badgeNumber,
                        "Duplicate badge number in batch: " + badgeNumber
                ));
                continue;
            }
            seenBadgeNumbers.add(badgeNumber);

            // Check for existing badge number (idempotency)
            if (officerConflictService.badgeNumberExists(badgeNumber)) {
                failed.add(new BatchCreateResponseDto.BatchFailure(
                        badgeNumber,
                        "Officer with badge number already exists: " + badgeNumber
                ));
                continue;
            }

            try {
                // Create and publish RegisterOfficerRequested event
                RegisterOfficerRequested event = new RegisterOfficerRequested(
                        badgeNumber,
                        officerDto.getFirstName(),
                        officerDto.getLastName(),
                        officerDto.getRank(),
                        officerDto.getEmail(),
                        officerDto.getPhoneNumber(),
                        officerDto.getHireDate() != null ? officerDto.getHireDate().toString() : null,
                        EnumConverter.convertEnumToString(officerDto.getStatus())
                );

                eventPublisher.publish(topicConfiguration.OFFICER_EVENTS, badgeNumber, event);
                created.add(badgeNumber);
            } catch (Exception e) {
                failed.add(new BatchCreateResponseDto.BatchFailure(
                        badgeNumber,
                        "Failed to register officer: " + e.getMessage()
                ));
            }
        }

        return new BatchCreateResponseDto(created, failed);
    }

    @Override
    public Class<BatchRegisterOfficersCommand> getCommandType() {
        return BatchRegisterOfficersCommand.class;
    }
}
