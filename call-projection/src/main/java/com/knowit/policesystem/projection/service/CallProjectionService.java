package com.knowit.policesystem.projection.service;

import com.knowit.policesystem.common.events.calls.ArriveAtCallRequested;
import com.knowit.policesystem.common.events.calls.ChangeCallStatusRequested;
import com.knowit.policesystem.common.events.calls.ClearCallRequested;
import com.knowit.policesystem.common.events.calls.DispatchCallRequested;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.common.events.calls.UpdateCallRequested;
import com.knowit.policesystem.projection.api.CallProjectionPageResponse;
import com.knowit.policesystem.projection.api.CallProjectionResponse;
import com.knowit.policesystem.projection.api.CallStatusHistoryResponse;
import com.knowit.policesystem.projection.model.CallProjectionEntity;
import com.knowit.policesystem.projection.repository.CallProjectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CallProjectionService {

    private static final Logger log = LoggerFactory.getLogger(CallProjectionService.class);

    private final CallProjectionRepository repository;
    private final Clock clock;

    public CallProjectionService(CallProjectionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void handle(Object event) {
        if (event instanceof ReceiveCallRequested receive) {
            handleRegister(receive);
            return;
        }
        if (event instanceof UpdateCallRequested update) {
            handleUpdate(update);
            return;
        }
        if (event instanceof ChangeCallStatusRequested changeStatus) {
            handleChangeStatus(changeStatus);
            return;
        }
        if (event instanceof DispatchCallRequested dispatch) {
            handleDispatch(dispatch);
            return;
        }
        if (event instanceof ArriveAtCallRequested arrive) {
            handleArrive(arrive);
            return;
        }
        if (event instanceof ClearCallRequested clear) {
            handleClear(clear);
            return;
        }
        log.warn("Received unsupported event type: {}", event != null ? event.getClass().getSimpleName() : "null");
    }

    public void handleRegister(ReceiveCallRequested event) {
        repository.upsert(event, now());
    }

    public void handleUpdate(UpdateCallRequested event) {
        repository.applyUpdate(event, now());
    }

    public void handleChangeStatus(ChangeCallStatusRequested event) {
        repository.changeStatus(event, now());
    }

    public void handleDispatch(DispatchCallRequested event) {
        repository.updateDispatchTime(event.getCallId(), event.getDispatchedTime(), now());
    }

    public void handleArrive(ArriveAtCallRequested event) {
        repository.updateArrivedTime(event.getCallId(), event.getArrivedTime(), now());
    }

    public void handleClear(ClearCallRequested event) {
        repository.updateClearedTime(event.getCallId(), event.getClearedTime(), now());
    }

    public Optional<CallProjectionResponse> getProjection(String callId) {
        return repository.findByCallId(callId).map(this::toResponse);
    }

    public List<CallStatusHistoryResponse> getHistory(String callId) {
        return repository.findHistory(callId).stream()
                .map(entry -> new CallStatusHistoryResponse(entry.status(), entry.changedAt()))
                .collect(Collectors.toList());
    }

    public CallProjectionPageResponse list(String status, String priority, String callType, int page, int size) {
        List<CallProjectionResponse> content = repository.findAll(status, priority, callType, page, size)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        long total = repository.count(status, priority, callType);
        return new CallProjectionPageResponse(content, total, page, size);
    }

    private CallProjectionResponse toResponse(CallProjectionEntity entity) {
        return new CallProjectionResponse(
                entity.callId(),
                entity.callNumber(),
                entity.priority(),
                entity.status(),
                entity.receivedTime(),
                entity.dispatchedTime(),
                entity.arrivedTime(),
                entity.clearedTime(),
                entity.description(),
                entity.callType(),
                entity.updatedAt()
        );
    }

    private Instant now() {
        return clock.instant();
    }
}

