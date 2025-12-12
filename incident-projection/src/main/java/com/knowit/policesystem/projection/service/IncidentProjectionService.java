package com.knowit.policesystem.projection.service;

import com.knowit.policesystem.common.events.incidents.ArriveAtIncidentRequested;
import com.knowit.policesystem.common.events.incidents.ChangeIncidentStatusRequested;
import com.knowit.policesystem.common.events.incidents.ClearIncidentRequested;
import com.knowit.policesystem.common.events.incidents.DispatchIncidentRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.common.events.incidents.UpdateIncidentRequested;
import com.knowit.policesystem.projection.api.IncidentProjectionPageResponse;
import com.knowit.policesystem.projection.api.IncidentProjectionResponse;
import com.knowit.policesystem.projection.api.IncidentStatusHistoryResponse;
import com.knowit.policesystem.projection.model.IncidentProjectionEntity;
import com.knowit.policesystem.projection.repository.IncidentProjectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class IncidentProjectionService {

    private static final Logger log = LoggerFactory.getLogger(IncidentProjectionService.class);

    private final IncidentProjectionRepository repository;
    private final Clock clock;

    public IncidentProjectionService(IncidentProjectionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void handle(Object event) {
        if (event instanceof ReportIncidentRequested report) {
            handleRegister(report);
            return;
        }
        if (event instanceof UpdateIncidentRequested update) {
            handleUpdate(update);
            return;
        }
        if (event instanceof ChangeIncidentStatusRequested changeStatus) {
            handleChangeStatus(changeStatus);
            return;
        }
        if (event instanceof DispatchIncidentRequested dispatch) {
            handleDispatch(dispatch);
            return;
        }
        if (event instanceof ArriveAtIncidentRequested arrive) {
            handleArrive(arrive);
            return;
        }
        if (event instanceof ClearIncidentRequested clear) {
            handleClear(clear);
            return;
        }
        log.warn("Received unsupported event type: {}", event != null ? event.getClass().getSimpleName() : "null");
    }

    public void handleRegister(ReportIncidentRequested event) {
        repository.upsert(event, now());
    }

    public void handleUpdate(UpdateIncidentRequested event) {
        repository.applyUpdate(event, now());
    }

    public void handleChangeStatus(ChangeIncidentStatusRequested event) {
        repository.changeStatus(event, now());
    }

    public void handleDispatch(DispatchIncidentRequested event) {
        repository.updateDispatchTime(event.getIncidentId(), event.getDispatchedTime(), now());
    }

    public void handleArrive(ArriveAtIncidentRequested event) {
        repository.updateArrivedTime(event.getIncidentId(), event.getArrivedTime(), now());
    }

    public void handleClear(ClearIncidentRequested event) {
        repository.updateClearedTime(event.getIncidentId(), event.getClearedTime(), now());
    }

    public Optional<IncidentProjectionResponse> getProjection(String incidentId) {
        return repository.findByIncidentId(incidentId).map(this::toResponse);
    }

    public List<IncidentStatusHistoryResponse> getHistory(String incidentId) {
        return repository.findHistory(incidentId).stream()
                .map(entry -> new IncidentStatusHistoryResponse(entry.status(), entry.changedAt()))
                .collect(Collectors.toList());
    }

    public IncidentProjectionPageResponse list(String status, String priority, String incidentType, int page, int size) {
        List<IncidentProjectionResponse> content = repository.findAll(status, priority, incidentType, page, size)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        long total = repository.count(status, priority, incidentType);
        return new IncidentProjectionPageResponse(content, total, page, size);
    }

    private IncidentProjectionResponse toResponse(IncidentProjectionEntity entity) {
        return new IncidentProjectionResponse(
                entity.incidentId(),
                entity.incidentNumber(),
                entity.priority(),
                entity.status(),
                entity.reportedTime(),
                entity.dispatchedTime(),
                entity.arrivedTime(),
                entity.clearedTime(),
                entity.description(),
                entity.incidentType(),
                entity.updatedAt()
        );
    }

    private Instant now() {
        return clock.instant();
    }
}
