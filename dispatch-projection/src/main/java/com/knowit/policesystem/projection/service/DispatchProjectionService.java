package com.knowit.policesystem.projection.service;

import com.knowit.policesystem.common.events.dispatches.ChangeDispatchStatusRequested;
import com.knowit.policesystem.common.events.dispatches.CreateDispatchRequested;
import com.knowit.policesystem.projection.api.DispatchProjectionPageResponse;
import com.knowit.policesystem.projection.api.DispatchProjectionResponse;
import com.knowit.policesystem.projection.api.DispatchStatusHistoryResponse;
import com.knowit.policesystem.projection.model.DispatchProjectionEntity;
import com.knowit.policesystem.projection.repository.DispatchProjectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DispatchProjectionService {

    private static final Logger log = LoggerFactory.getLogger(DispatchProjectionService.class);

    private final DispatchProjectionRepository repository;
    private final Clock clock;

    public DispatchProjectionService(DispatchProjectionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void handle(Object event) {
        if (event instanceof CreateDispatchRequested create) {
            handleCreate(create);
            return;
        }
        if (event instanceof ChangeDispatchStatusRequested changeStatus) {
            handleChangeStatus(changeStatus);
            return;
        }
        log.warn("Received unsupported event type: {}", event != null ? event.getClass().getSimpleName() : "null");
    }

    public void handleCreate(CreateDispatchRequested event) {
        repository.upsert(event, now());
    }

    public void handleChangeStatus(ChangeDispatchStatusRequested event) {
        repository.changeStatus(event, now());
    }

    public Optional<DispatchProjectionResponse> getProjection(String dispatchId) {
        return repository.findByDispatchId(dispatchId).map(this::toResponse);
    }

    public List<DispatchStatusHistoryResponse> getHistory(String dispatchId) {
        return repository.findHistory(dispatchId).stream()
                .map(entry -> new DispatchStatusHistoryResponse(entry.status(), entry.changedAt()))
                .collect(Collectors.toList());
    }

    public DispatchProjectionPageResponse list(String status, String dispatchType, int page, int size) {
        List<DispatchProjectionResponse> content = repository.findAll(status, dispatchType, page, size)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        long total = repository.count(status, dispatchType);
        return new DispatchProjectionPageResponse(content, total, page, size);
    }

    private DispatchProjectionResponse toResponse(DispatchProjectionEntity entity) {
        return new DispatchProjectionResponse(
                entity.dispatchId(),
                entity.dispatchTime(),
                entity.dispatchType(),
                entity.status(),
                entity.updatedAt()
        );
    }

    private Instant now() {
        return clock.instant();
    }
}

