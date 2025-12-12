package com.knowit.policesystem.projection.service;

import com.knowit.policesystem.common.events.activities.ChangeActivityStatusRequested;
import com.knowit.policesystem.common.events.activities.CompleteActivityRequested;
import com.knowit.policesystem.common.events.activities.StartActivityRequested;
import com.knowit.policesystem.common.events.activities.UpdateActivityRequested;
import com.knowit.policesystem.projection.api.ActivityProjectionPageResponse;
import com.knowit.policesystem.projection.api.ActivityProjectionResponse;
import com.knowit.policesystem.projection.api.ActivityStatusHistoryResponse;
import com.knowit.policesystem.projection.model.ActivityProjectionEntity;
import com.knowit.policesystem.projection.repository.ActivityProjectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActivityProjectionService {

    private static final Logger log = LoggerFactory.getLogger(ActivityProjectionService.class);

    private final ActivityProjectionRepository repository;
    private final Clock clock;

    public ActivityProjectionService(ActivityProjectionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void handle(Object event) {
        if (event instanceof StartActivityRequested start) {
            handleStart(start);
            return;
        }
        if (event instanceof UpdateActivityRequested update) {
            handleUpdate(update);
            return;
        }
        if (event instanceof ChangeActivityStatusRequested changeStatus) {
            handleChangeStatus(changeStatus);
            return;
        }
        if (event instanceof CompleteActivityRequested complete) {
            handleComplete(complete);
            return;
        }
        log.warn("Received unsupported event type: {}", event != null ? event.getClass().getSimpleName() : "null");
    }

    public void handleStart(StartActivityRequested event) {
        repository.upsert(event, now());
    }

    public void handleUpdate(UpdateActivityRequested event) {
        repository.applyUpdate(event, now());
    }

    public void handleChangeStatus(ChangeActivityStatusRequested event) {
        repository.changeStatus(event, now());
    }

    public void handleComplete(CompleteActivityRequested event) {
        repository.updateCompletedTime(event.getActivityId(), event.getCompletedTime(), now());
    }

    public Optional<ActivityProjectionResponse> getProjection(String activityId) {
        return repository.findByActivityId(activityId).map(this::toResponse);
    }

    public List<ActivityStatusHistoryResponse> getHistory(String activityId) {
        return repository.findHistory(activityId).stream()
                .map(entry -> new ActivityStatusHistoryResponse(entry.status(), entry.changedAt()))
                .collect(Collectors.toList());
    }

    public ActivityProjectionPageResponse list(String status, String activityType, int page, int size) {
        List<ActivityProjectionResponse> content = repository.findAll(status, activityType, page, size)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        long total = repository.count(status, activityType);
        return new ActivityProjectionPageResponse(content, total, page, size);
    }

    private ActivityProjectionResponse toResponse(ActivityProjectionEntity entity) {
        return new ActivityProjectionResponse(
                entity.activityId(),
                entity.activityTime(),
                entity.activityType(),
                entity.description(),
                entity.status(),
                entity.completedTime(),
                entity.updatedAt()
        );
    }

    private Instant now() {
        return clock.instant();
    }
}

