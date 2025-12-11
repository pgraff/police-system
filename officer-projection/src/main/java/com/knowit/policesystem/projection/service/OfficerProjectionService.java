package com.knowit.policesystem.projection.service;

import com.knowit.policesystem.common.events.officers.ChangeOfficerStatusRequested;
import com.knowit.policesystem.common.events.officers.RegisterOfficerRequested;
import com.knowit.policesystem.common.events.officers.UpdateOfficerRequested;
import com.knowit.policesystem.projection.api.OfficerProjectionPageResponse;
import com.knowit.policesystem.projection.api.OfficerProjectionResponse;
import com.knowit.policesystem.projection.api.OfficerStatusHistoryResponse;
import com.knowit.policesystem.projection.model.OfficerProjectionEntity;
import com.knowit.policesystem.projection.repository.OfficerProjectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OfficerProjectionService {

    private static final Logger log = LoggerFactory.getLogger(OfficerProjectionService.class);

    private final OfficerProjectionRepository repository;
    private final Clock clock;

    public OfficerProjectionService(OfficerProjectionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void handle(Object event) {
        if (event instanceof RegisterOfficerRequested register) {
            handleRegister(register);
            return;
        }
        if (event instanceof UpdateOfficerRequested update) {
            handleUpdate(update);
            return;
        }
        if (event instanceof ChangeOfficerStatusRequested changeStatus) {
            handleChangeStatus(changeStatus);
            return;
        }
        log.warn("Received unsupported event type: {}", event != null ? event.getClass().getSimpleName() : "null");
    }

    public void handleRegister(RegisterOfficerRequested event) {
        repository.upsert(event, now());
    }

    public void handleUpdate(UpdateOfficerRequested event) {
        repository.applyUpdate(event, now());
    }

    public void handleChangeStatus(ChangeOfficerStatusRequested event) {
        repository.changeStatus(event, now());
    }

    public Optional<OfficerProjectionResponse> getProjection(String badgeNumber) {
        return repository.findByBadgeNumber(badgeNumber).map(this::toResponse);
    }

    public List<OfficerStatusHistoryResponse> getHistory(String badgeNumber) {
        return repository.findHistory(badgeNumber).stream()
                .map(entry -> new OfficerStatusHistoryResponse(entry.status(), entry.changedAt()))
                .collect(Collectors.toList());
    }

    public OfficerProjectionPageResponse list(String status, String rank, int page, int size) {
        List<OfficerProjectionResponse> content = repository.findAll(status, rank, page, size)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        long total = repository.count(status, rank);
        return new OfficerProjectionPageResponse(content, total, page, size);
    }

    private OfficerProjectionResponse toResponse(OfficerProjectionEntity entity) {
        return new OfficerProjectionResponse(
                entity.badgeNumber(),
                entity.firstName(),
                entity.lastName(),
                entity.rank(),
                entity.email(),
                entity.phoneNumber(),
                entity.hireDate(),
                entity.status(),
                entity.updatedAt()
        );
    }

    private Instant now() {
        return clock.instant();
    }
}
