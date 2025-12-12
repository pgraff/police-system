package com.knowit.policesystem.projection.service;

import com.knowit.policesystem.common.events.assignments.ChangeAssignmentStatusRequested;
import com.knowit.policesystem.common.events.assignments.CompleteAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.CreateAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.LinkAssignmentToDispatchRequested;
import com.knowit.policesystem.common.events.resourceassignment.AssignResourceRequested;
import com.knowit.policesystem.projection.api.AssignmentProjectionPageResponse;
import com.knowit.policesystem.projection.api.AssignmentProjectionResponse;
import com.knowit.policesystem.projection.api.AssignmentResourceResponse;
import com.knowit.policesystem.projection.api.AssignmentStatusHistoryResponse;
import com.knowit.policesystem.projection.model.AssignmentProjectionEntity;
import com.knowit.policesystem.projection.model.AssignmentResourceEntry;
import com.knowit.policesystem.projection.model.AssignmentStatusHistoryEntry;
import com.knowit.policesystem.projection.repository.AssignmentProjectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AssignmentProjectionService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentProjectionService.class);

    private final AssignmentProjectionRepository repository;
    private final Clock clock;

    public AssignmentProjectionService(AssignmentProjectionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void handle(Object event) {
        if (event instanceof CreateAssignmentRequested create) {
            handleCreate(create);
            return;
        }
        if (event instanceof ChangeAssignmentStatusRequested changeStatus) {
            handleChangeStatus(changeStatus);
            return;
        }
        if (event instanceof CompleteAssignmentRequested complete) {
            handleComplete(complete);
            return;
        }
        if (event instanceof LinkAssignmentToDispatchRequested link) {
            handleLinkDispatch(link);
            return;
        }
        if (event instanceof AssignResourceRequested assignResource) {
            handleAssignResource(assignResource);
            return;
        }
        log.warn("Received unsupported event type: {}", event != null ? event.getClass().getSimpleName() : "null");
    }

    public void handleCreate(CreateAssignmentRequested event) {
        validateIncidentOrCall(event.getIncidentId(), event.getCallId());
        repository.upsert(event, now());
    }

    public void handleChangeStatus(ChangeAssignmentStatusRequested event) {
        repository.changeStatus(event, now());
    }

    public void handleComplete(CompleteAssignmentRequested event) {
        repository.complete(event, now());
    }

    public void handleLinkDispatch(LinkAssignmentToDispatchRequested event) {
        repository.linkDispatch(event, now());
    }

    public void handleAssignResource(AssignResourceRequested event) {
        repository.upsertResource(event, now());
    }

    public Optional<AssignmentProjectionResponse> getProjection(String assignmentId) {
        return repository.findByAssignmentId(assignmentId).map(this::toResponse);
    }

    public List<AssignmentStatusHistoryResponse> getHistory(String assignmentId) {
        return repository.findHistory(assignmentId).stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }

    public List<AssignmentResourceResponse> getResources(String assignmentId) {
        return repository.findResources(assignmentId).stream()
                .map(this::toResourceResponse)
                .collect(Collectors.toList());
    }

    public AssignmentProjectionPageResponse list(String status, String assignmentType, String dispatchId,
                                                 String incidentId, String callId, String resourceId,
                                                 int page, int size) {
        List<AssignmentProjectionResponse> content = repository.findAll(
                        status, assignmentType, dispatchId, incidentId, callId, resourceId, page, size)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        long total = repository.count(status, assignmentType, dispatchId, incidentId, callId, resourceId);
        return new AssignmentProjectionPageResponse(content, total, page, size);
    }

    private AssignmentProjectionResponse toResponse(AssignmentProjectionEntity entity) {
        return new AssignmentProjectionResponse(
                entity.assignmentId(),
                entity.assignedTime(),
                entity.assignmentType(),
                entity.status(),
                entity.incidentId(),
                entity.callId(),
                entity.dispatchId(),
                entity.completedTime(),
                entity.createdAt(),
                entity.updatedAt()
        );
    }

    private AssignmentStatusHistoryResponse toHistoryResponse(AssignmentStatusHistoryEntry entry) {
        return new AssignmentStatusHistoryResponse(entry.status(), entry.changedAt());
    }

    private AssignmentResourceResponse toResourceResponse(AssignmentResourceEntry entry) {
        return new AssignmentResourceResponse(
                entry.id(),
                entry.assignmentId(),
                entry.resourceId(),
                entry.resourceType(),
                entry.roleType(),
                entry.status(),
                entry.startTime(),
                entry.updatedAt()
        );
    }

    private void validateIncidentOrCall(String incidentId, String callId) {
        if (incidentId != null && callId != null) {
            throw new IllegalArgumentException("incidentId and callId are mutually exclusive");
        }
    }

    private Instant now() {
        return clock.instant();
    }
}
