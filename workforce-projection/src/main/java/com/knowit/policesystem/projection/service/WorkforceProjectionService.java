package com.knowit.policesystem.projection.service;

import com.knowit.policesystem.common.events.officershifts.CheckInOfficerRequested;
import com.knowit.policesystem.common.events.officershifts.CheckOutOfficerRequested;
import com.knowit.policesystem.common.events.officershifts.UpdateOfficerShiftRequested;
import com.knowit.policesystem.common.events.shifts.ChangeShiftStatusRequested;
import com.knowit.policesystem.common.events.shifts.EndShiftRequested;
import com.knowit.policesystem.common.events.shifts.RecordShiftChangeRequested;
import com.knowit.policesystem.common.events.shifts.StartShiftRequested;
import com.knowit.policesystem.projection.api.OfficerShiftProjectionPageResponse;
import com.knowit.policesystem.projection.api.OfficerShiftProjectionResponse;
import com.knowit.policesystem.projection.api.ShiftChangeProjectionPageResponse;
import com.knowit.policesystem.projection.api.ShiftChangeProjectionResponse;
import com.knowit.policesystem.projection.api.ShiftProjectionPageResponse;
import com.knowit.policesystem.projection.api.ShiftProjectionResponse;
import com.knowit.policesystem.projection.api.ShiftStatusHistoryResponse;
import com.knowit.policesystem.projection.model.OfficerShiftProjectionEntity;
import com.knowit.policesystem.projection.model.ShiftChangeProjectionEntity;
import com.knowit.policesystem.projection.model.ShiftProjectionEntity;
import com.knowit.policesystem.projection.model.ShiftStatusHistoryEntry;
import com.knowit.policesystem.projection.repository.OfficerShiftProjectionRepository;
import com.knowit.policesystem.projection.repository.ShiftChangeProjectionRepository;
import com.knowit.policesystem.projection.repository.ShiftProjectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for handling workforce projection events.
 * Routes all workforce events to appropriate repositories.
 */
@Service
public class WorkforceProjectionService {

    private static final Logger log = LoggerFactory.getLogger(WorkforceProjectionService.class);

    private final ShiftProjectionRepository shiftRepository;
    private final OfficerShiftProjectionRepository officerShiftRepository;
    private final ShiftChangeProjectionRepository shiftChangeRepository;
    private final Clock clock;

    public WorkforceProjectionService(
            ShiftProjectionRepository shiftRepository,
            OfficerShiftProjectionRepository officerShiftRepository,
            ShiftChangeProjectionRepository shiftChangeRepository,
            Clock clock) {
        this.shiftRepository = shiftRepository;
        this.officerShiftRepository = officerShiftRepository;
        this.shiftChangeRepository = shiftChangeRepository;
        this.clock = clock;
    }

    public void handle(Object event) {
        if (event == null) {
            log.warn("Received null event");
            return;
        }

        // Shift events
        if (event instanceof StartShiftRequested start) {
            handleStartShift(start);
            return;
        }
        if (event instanceof EndShiftRequested end) {
            handleEndShift(end);
            return;
        }
        if (event instanceof ChangeShiftStatusRequested changeStatus) {
            handleChangeShiftStatus(changeStatus);
            return;
        }
        if (event instanceof RecordShiftChangeRequested recordChange) {
            handleRecordShiftChange(recordChange);
            return;
        }

        // Officer shift events
        if (event instanceof CheckInOfficerRequested checkIn) {
            handleCheckInOfficer(checkIn);
            return;
        }
        if (event instanceof CheckOutOfficerRequested checkOut) {
            handleCheckOutOfficer(checkOut);
            return;
        }
        if (event instanceof UpdateOfficerShiftRequested update) {
            handleUpdateOfficerShift(update);
            return;
        }

        log.warn("Received unsupported event type: {}", event.getClass().getSimpleName());
    }

    // Shift event handlers
    private void handleStartShift(StartShiftRequested event) {
        shiftRepository.upsert(event, now());
    }

    private void handleEndShift(EndShiftRequested event) {
        shiftRepository.applyUpdate(event, now());
    }

    private void handleChangeShiftStatus(ChangeShiftStatusRequested event) {
        shiftRepository.changeStatus(event, now());
    }

    private void handleRecordShiftChange(RecordShiftChangeRequested event) {
        shiftChangeRepository.upsert(event, now());
    }

    // Officer shift event handlers
    private void handleCheckInOfficer(CheckInOfficerRequested event) {
        officerShiftRepository.upsert(event, now());
    }

    private void handleCheckOutOfficer(CheckOutOfficerRequested event) {
        officerShiftRepository.applyUpdate(event, now());
    }

    private void handleUpdateOfficerShift(UpdateOfficerShiftRequested event) {
        officerShiftRepository.applyUpdate(event, now());
    }

    // Shift query methods
    public Optional<ShiftProjectionResponse> getShift(String shiftId) {
        return shiftRepository.findByShiftId(shiftId)
                .map(this::toShiftResponse);
    }

    public List<ShiftStatusHistoryResponse> getShiftHistory(String shiftId) {
        return shiftRepository.findHistory(shiftId).stream()
                .map(this::toShiftStatusHistoryResponse)
                .collect(Collectors.toList());
    }

    public ShiftProjectionPageResponse listShifts(String status, String shiftType, int page, int size) {
        List<ShiftProjectionResponse> content = shiftRepository.findAll(status, shiftType, page, size).stream()
                .map(this::toShiftResponse)
                .collect(Collectors.toList());
        long total = shiftRepository.count(status, shiftType);
        return new ShiftProjectionPageResponse(content, total, page, size);
    }

    // Officer shift query methods
    public Optional<OfficerShiftProjectionResponse> getOfficerShift(Long id) {
        return officerShiftRepository.findById(id)
                .map(this::toOfficerShiftResponse);
    }

    public OfficerShiftProjectionPageResponse listOfficerShifts(String shiftId, String badgeNumber, int page, int size) {
        List<OfficerShiftProjectionResponse> content = officerShiftRepository.findAll(shiftId, badgeNumber, page, size).stream()
                .map(this::toOfficerShiftResponse)
                .collect(Collectors.toList());
        long total = officerShiftRepository.count(shiftId, badgeNumber);
        return new OfficerShiftProjectionPageResponse(content, total, page, size);
    }

    // Shift change query methods
    public Optional<ShiftChangeProjectionResponse> getShiftChange(String shiftChangeId) {
        return shiftChangeRepository.findByShiftChangeId(shiftChangeId)
                .map(this::toShiftChangeResponse);
    }

    public ShiftChangeProjectionPageResponse listShiftChanges(String shiftId, String changeType, int page, int size) {
        List<ShiftChangeProjectionResponse> content = shiftChangeRepository.findAll(shiftId, changeType, page, size).stream()
                .map(this::toShiftChangeResponse)
                .collect(Collectors.toList());
        long total = shiftChangeRepository.count(shiftId, changeType);
        return new ShiftChangeProjectionPageResponse(content, total, page, size);
    }

    // Mapper methods
    private ShiftProjectionResponse toShiftResponse(ShiftProjectionEntity entity) {
        return new ShiftProjectionResponse(
                entity.shiftId(),
                entity.startTime(),
                entity.endTime(),
                entity.shiftType(),
                entity.status(),
                entity.updatedAt()
        );
    }

    private ShiftStatusHistoryResponse toShiftStatusHistoryResponse(ShiftStatusHistoryEntry entry) {
        return new ShiftStatusHistoryResponse(entry.status(), entry.changedAt());
    }

    private OfficerShiftProjectionResponse toOfficerShiftResponse(OfficerShiftProjectionEntity entity) {
        return new OfficerShiftProjectionResponse(
                entity.id(),
                entity.shiftId(),
                entity.badgeNumber(),
                entity.checkInTime(),
                entity.checkOutTime(),
                entity.shiftRoleType(),
                entity.updatedAt()
        );
    }

    private ShiftChangeProjectionResponse toShiftChangeResponse(ShiftChangeProjectionEntity entity) {
        return new ShiftChangeProjectionResponse(
                entity.shiftChangeId(),
                entity.shiftId(),
                entity.changeTime(),
                entity.changeType(),
                entity.notes(),
                entity.updatedAt()
        );
    }

    private Instant now() {
        return clock.instant();
    }
}
