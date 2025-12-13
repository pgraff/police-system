package com.knowit.policesystem.projection.service;

import com.knowit.policesystem.common.events.activities.ChangeActivityStatusRequested;
import com.knowit.policesystem.common.events.activities.CompleteActivityRequested;
import com.knowit.policesystem.common.events.activities.LinkActivityToIncidentRequested;
import com.knowit.policesystem.common.events.activities.StartActivityRequested;
import com.knowit.policesystem.common.events.activities.UpdateActivityRequested;
import com.knowit.policesystem.common.events.assignments.ChangeAssignmentStatusRequested;
import com.knowit.policesystem.common.events.assignments.CompleteAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.CreateAssignmentRequested;
import com.knowit.policesystem.common.events.assignments.LinkAssignmentToDispatchRequested;
import com.knowit.policesystem.common.events.calls.ArriveAtCallRequested;
import com.knowit.policesystem.common.events.calls.ChangeCallStatusRequested;
import com.knowit.policesystem.common.events.calls.ClearCallRequested;
import com.knowit.policesystem.common.events.calls.DispatchCallRequested;
import com.knowit.policesystem.common.events.calls.LinkCallToDispatchRequested;
import com.knowit.policesystem.common.events.calls.LinkCallToIncidentRequested;
import com.knowit.policesystem.common.events.calls.ReceiveCallRequested;
import com.knowit.policesystem.common.events.calls.UpdateCallRequested;
import com.knowit.policesystem.common.events.dispatches.ChangeDispatchStatusRequested;
import com.knowit.policesystem.common.events.dispatches.CreateDispatchRequested;
import com.knowit.policesystem.common.events.incidents.ArriveAtIncidentRequested;
import com.knowit.policesystem.common.events.incidents.ChangeIncidentStatusRequested;
import com.knowit.policesystem.common.events.incidents.ClearIncidentRequested;
import com.knowit.policesystem.common.events.incidents.DispatchIncidentRequested;
import com.knowit.policesystem.common.events.incidents.ReportIncidentRequested;
import com.knowit.policesystem.common.events.incidents.UpdateIncidentRequested;
import com.knowit.policesystem.common.events.involvedparty.EndPartyInvolvementRequested;
import com.knowit.policesystem.common.events.involvedparty.InvolvePartyRequested;
import com.knowit.policesystem.common.events.involvedparty.UpdatePartyInvolvementRequested;
import com.knowit.policesystem.common.events.resourceassignment.AssignResourceRequested;
import com.knowit.policesystem.common.events.resourceassignment.ChangeResourceAssignmentStatusRequested;
import com.knowit.policesystem.common.events.resourceassignment.UnassignResourceRequested;
import com.knowit.policesystem.projection.api.ActivityProjectionPageResponse;
import com.knowit.policesystem.projection.api.ActivityProjectionResponse;
import com.knowit.policesystem.projection.api.ActivityStatusHistoryResponse;
import com.knowit.policesystem.projection.api.AssignmentProjectionPageResponse;
import com.knowit.policesystem.projection.api.AssignmentProjectionResponse;
import com.knowit.policesystem.projection.api.AssignmentStatusHistoryResponse;
import com.knowit.policesystem.projection.api.AssignmentWithResourcesResponse;
import com.knowit.policesystem.projection.api.CallProjectionPageResponse;
import com.knowit.policesystem.projection.api.CallProjectionResponse;
import com.knowit.policesystem.projection.api.CallStatusHistoryResponse;
import com.knowit.policesystem.projection.api.CallWithDispatchesResponse;
import com.knowit.policesystem.projection.api.DispatchProjectionPageResponse;
import com.knowit.policesystem.projection.api.DispatchProjectionResponse;
import com.knowit.policesystem.projection.api.DispatchStatusHistoryResponse;
import com.knowit.policesystem.projection.api.IncidentFullResponse;
import com.knowit.policesystem.projection.api.IncidentProjectionPageResponse;
import com.knowit.policesystem.projection.api.IncidentProjectionResponse;
import com.knowit.policesystem.projection.api.IncidentStatusHistoryResponse;
import com.knowit.policesystem.projection.api.InvolvedPartyProjectionPageResponse;
import com.knowit.policesystem.projection.api.InvolvedPartyProjectionResponse;
import com.knowit.policesystem.projection.api.ResourceAssignmentProjectionPageResponse;
import com.knowit.policesystem.projection.api.ResourceAssignmentProjectionResponse;
import com.knowit.policesystem.projection.model.ActivityProjectionEntity;
import com.knowit.policesystem.projection.model.ActivityStatusHistoryEntry;
import com.knowit.policesystem.projection.model.AssignmentProjectionEntity;
import com.knowit.policesystem.projection.model.AssignmentStatusHistoryEntry;
import com.knowit.policesystem.projection.model.CallProjectionEntity;
import com.knowit.policesystem.projection.model.CallStatusHistoryEntry;
import com.knowit.policesystem.projection.model.DispatchProjectionEntity;
import com.knowit.policesystem.projection.model.DispatchStatusHistoryEntry;
import com.knowit.policesystem.projection.model.IncidentProjectionEntity;
import com.knowit.policesystem.projection.model.IncidentStatusHistoryEntry;
import com.knowit.policesystem.projection.model.InvolvedPartyProjectionEntity;
import com.knowit.policesystem.projection.model.ResourceAssignmentProjectionEntity;
import com.knowit.policesystem.projection.repository.ActivityProjectionRepository;
import com.knowit.policesystem.projection.repository.AssignmentProjectionRepository;
import com.knowit.policesystem.projection.repository.CallProjectionRepository;
import com.knowit.policesystem.projection.repository.DispatchProjectionRepository;
import com.knowit.policesystem.projection.repository.IncidentProjectionRepository;
import com.knowit.policesystem.projection.repository.InvolvedPartyProjectionRepository;
import com.knowit.policesystem.projection.repository.ResourceAssignmentProjectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for handling operational projection events.
 * Routes all operational events to appropriate repositories.
 */
@Service
public class OperationalProjectionService {

    private static final Logger log = LoggerFactory.getLogger(OperationalProjectionService.class);

    private final IncidentProjectionRepository incidentRepository;
    private final CallProjectionRepository callRepository;
    private final DispatchProjectionRepository dispatchRepository;
    private final ActivityProjectionRepository activityRepository;
    private final AssignmentProjectionRepository assignmentRepository;
    private final InvolvedPartyProjectionRepository involvedPartyRepository;
    private final ResourceAssignmentProjectionRepository resourceAssignmentRepository;
    private final Clock clock;

    public OperationalProjectionService(
            IncidentProjectionRepository incidentRepository,
            CallProjectionRepository callRepository,
            DispatchProjectionRepository dispatchRepository,
            ActivityProjectionRepository activityRepository,
            AssignmentProjectionRepository assignmentRepository,
            InvolvedPartyProjectionRepository involvedPartyRepository,
            ResourceAssignmentProjectionRepository resourceAssignmentRepository,
            Clock clock) {
        this.incidentRepository = incidentRepository;
        this.callRepository = callRepository;
        this.dispatchRepository = dispatchRepository;
        this.activityRepository = activityRepository;
        this.assignmentRepository = assignmentRepository;
        this.involvedPartyRepository = involvedPartyRepository;
        this.resourceAssignmentRepository = resourceAssignmentRepository;
        this.clock = clock;
    }

    public void handle(Object event) {
        if (event == null) {
            log.warn("Received null event");
            return;
        }

        // Incident events
        if (event instanceof ReportIncidentRequested report) {
            handleReportIncident(report);
            return;
        }
        if (event instanceof UpdateIncidentRequested update) {
            handleUpdateIncident(update);
            return;
        }
        if (event instanceof ChangeIncidentStatusRequested changeStatus) {
            handleChangeIncidentStatus(changeStatus);
            return;
        }
        if (event instanceof DispatchIncidentRequested dispatch) {
            handleDispatchIncident(dispatch);
            return;
        }
        if (event instanceof ArriveAtIncidentRequested arrive) {
            handleArriveAtIncident(arrive);
            return;
        }
        if (event instanceof ClearIncidentRequested clear) {
            handleClearIncident(clear);
            return;
        }

        // Call events
        if (event instanceof ReceiveCallRequested receive) {
            handleReceiveCall(receive);
            return;
        }
        if (event instanceof UpdateCallRequested update) {
            handleUpdateCall(update);
            return;
        }
        if (event instanceof ChangeCallStatusRequested changeStatus) {
            handleChangeCallStatus(changeStatus);
            return;
        }
        if (event instanceof DispatchCallRequested dispatch) {
            handleDispatchCall(dispatch);
            return;
        }
        if (event instanceof ArriveAtCallRequested arrive) {
            handleArriveAtCall(arrive);
            return;
        }
        if (event instanceof ClearCallRequested clear) {
            handleClearCall(clear);
            return;
        }
        if (event instanceof LinkCallToIncidentRequested link) {
            handleLinkCallToIncident(link);
            return;
        }
        if (event instanceof LinkCallToDispatchRequested link) {
            handleLinkCallToDispatch(link);
            return;
        }

        // Dispatch events
        if (event instanceof CreateDispatchRequested create) {
            handleCreateDispatch(create);
            return;
        }
        if (event instanceof ChangeDispatchStatusRequested changeStatus) {
            handleChangeDispatchStatus(changeStatus);
            return;
        }

        // Activity events
        if (event instanceof StartActivityRequested start) {
            handleStartActivity(start);
            return;
        }
        if (event instanceof UpdateActivityRequested update) {
            handleUpdateActivity(update);
            return;
        }
        if (event instanceof ChangeActivityStatusRequested changeStatus) {
            handleChangeActivityStatus(changeStatus);
            return;
        }
        if (event instanceof CompleteActivityRequested complete) {
            handleCompleteActivity(complete);
            return;
        }
        if (event instanceof LinkActivityToIncidentRequested link) {
            handleLinkActivityToIncident(link);
            return;
        }

        // Assignment events
        if (event instanceof CreateAssignmentRequested create) {
            handleCreateAssignment(create);
            return;
        }
        if (event instanceof ChangeAssignmentStatusRequested changeStatus) {
            handleChangeAssignmentStatus(changeStatus);
            return;
        }
        if (event instanceof CompleteAssignmentRequested complete) {
            handleCompleteAssignment(complete);
            return;
        }
        if (event instanceof LinkAssignmentToDispatchRequested link) {
            handleLinkAssignmentToDispatch(link);
            return;
        }

        // Involved party events
        if (event instanceof InvolvePartyRequested involve) {
            handleInvolveParty(involve);
            return;
        }
        if (event instanceof UpdatePartyInvolvementRequested update) {
            handleUpdatePartyInvolvement(update);
            return;
        }
        if (event instanceof EndPartyInvolvementRequested end) {
            handleEndPartyInvolvement(end);
            return;
        }

        // Resource assignment events
        if (event instanceof AssignResourceRequested assign) {
            handleAssignResource(assign);
            return;
        }
        if (event instanceof ChangeResourceAssignmentStatusRequested changeStatus) {
            handleChangeResourceAssignmentStatus(changeStatus);
            return;
        }
        if (event instanceof UnassignResourceRequested unassign) {
            handleUnassignResource(unassign);
            return;
        }

        log.warn("Received unsupported event type: {}", event.getClass().getSimpleName());
    }

    // Incident event handlers
    private void handleReportIncident(ReportIncidentRequested event) {
        incidentRepository.upsert(event, now());
    }

    private void handleUpdateIncident(UpdateIncidentRequested event) {
        incidentRepository.applyUpdate(event, now());
    }

    private void handleChangeIncidentStatus(ChangeIncidentStatusRequested event) {
        incidentRepository.changeStatus(event, now());
    }

    private void handleDispatchIncident(DispatchIncidentRequested event) {
        incidentRepository.updateDispatchTime(event.getIncidentId(), event.getDispatchedTime(), now());
    }

    private void handleArriveAtIncident(ArriveAtIncidentRequested event) {
        incidentRepository.updateArrivedTime(event.getIncidentId(), event.getArrivedTime(), now());
    }

    private void handleClearIncident(ClearIncidentRequested event) {
        incidentRepository.updateClearedTime(event.getIncidentId(), event.getClearedTime(), now());
    }

    // Call event handlers
    private void handleReceiveCall(ReceiveCallRequested event) {
        callRepository.upsert(event, now());
    }

    private void handleUpdateCall(UpdateCallRequested event) {
        callRepository.applyUpdate(event, now());
    }

    private void handleChangeCallStatus(ChangeCallStatusRequested event) {
        callRepository.changeStatus(event, now());
    }

    private void handleDispatchCall(DispatchCallRequested event) {
        callRepository.updateDispatchTime(event.getCallId(), event.getDispatchedTime(), now());
    }

    private void handleArriveAtCall(ArriveAtCallRequested event) {
        callRepository.updateArrivedTime(event.getCallId(), event.getArrivedTime(), now());
    }

    private void handleClearCall(ClearCallRequested event) {
        callRepository.updateClearedTime(event.getCallId(), event.getClearedTime(), now());
    }

    private void handleLinkCallToIncident(LinkCallToIncidentRequested event) {
        callRepository.linkToIncident(event, now());
    }

    private void handleLinkCallToDispatch(LinkCallToDispatchRequested event) {
        // This is a no-op in CallProjectionRepository (handled by DispatchProjectionRepository)
        // Still handle it to acknowledge the event was processed
        log.debug("LinkCallToDispatchRequested event received - handled by DispatchProjectionRepository");
    }

    // Dispatch event handlers
    private void handleCreateDispatch(CreateDispatchRequested event) {
        dispatchRepository.upsert(event, now());
    }

    private void handleChangeDispatchStatus(ChangeDispatchStatusRequested event) {
        dispatchRepository.changeStatus(event, now());
    }

    // Activity event handlers
    private void handleStartActivity(StartActivityRequested event) {
        activityRepository.upsert(event, now());
    }

    private void handleUpdateActivity(UpdateActivityRequested event) {
        activityRepository.applyUpdate(event, now());
    }

    private void handleChangeActivityStatus(ChangeActivityStatusRequested event) {
        activityRepository.changeStatus(event, now());
    }

    private void handleCompleteActivity(CompleteActivityRequested event) {
        activityRepository.updateCompletedTime(event.getActivityId(), event.getCompletedTime(), now());
    }

    private void handleLinkActivityToIncident(LinkActivityToIncidentRequested event) {
        activityRepository.linkToIncident(event, now());
    }

    // Assignment event handlers
    private void handleCreateAssignment(CreateAssignmentRequested event) {
        assignmentRepository.upsert(event, now());
    }

    private void handleChangeAssignmentStatus(ChangeAssignmentStatusRequested event) {
        assignmentRepository.changeStatus(event, now());
    }

    private void handleCompleteAssignment(CompleteAssignmentRequested event) {
        assignmentRepository.complete(event, now());
    }

    private void handleLinkAssignmentToDispatch(LinkAssignmentToDispatchRequested event) {
        assignmentRepository.linkDispatch(event, now());
    }

    // Involved party event handlers
    private void handleInvolveParty(InvolvePartyRequested event) {
        involvedPartyRepository.upsert(event, now());
    }

    private void handleUpdatePartyInvolvement(UpdatePartyInvolvementRequested event) {
        involvedPartyRepository.applyUpdate(event, now());
    }

    private void handleEndPartyInvolvement(EndPartyInvolvementRequested event) {
        involvedPartyRepository.endInvolvement(event, now());
    }

    // Resource assignment event handlers
    private void handleAssignResource(AssignResourceRequested event) {
        resourceAssignmentRepository.upsert(event, now());
    }

    private void handleChangeResourceAssignmentStatus(ChangeResourceAssignmentStatusRequested event) {
        resourceAssignmentRepository.changeStatus(event, now());
    }

    private void handleUnassignResource(UnassignResourceRequested event) {
        resourceAssignmentRepository.unassign(event, now());
    }

    // Query methods - Incidents
    public Optional<IncidentProjectionResponse> getIncident(String incidentId) {
        return incidentRepository.findByIncidentId(incidentId).map(this::toIncidentResponse);
    }

    public List<IncidentStatusHistoryResponse> getIncidentHistory(String incidentId) {
        return incidentRepository.findHistory(incidentId).stream()
                .map(entry -> new IncidentStatusHistoryResponse(entry.status(), entry.changedAt()))
                .collect(Collectors.toList());
    }

    public IncidentProjectionPageResponse listIncidents(String status, String priority, String incidentType, int page, int size) {
        List<IncidentProjectionResponse> content = incidentRepository.findAll(status, priority, incidentType, page, size)
                .stream()
                .map(this::toIncidentResponse)
                .collect(Collectors.toList());
        long total = incidentRepository.count(status, priority, incidentType);
        return new IncidentProjectionPageResponse(content, total, page, size);
    }

    private IncidentProjectionResponse toIncidentResponse(IncidentProjectionEntity entity) {
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

    // Query methods - Calls
    public Optional<CallProjectionResponse> getCall(String callId) {
        return callRepository.findByCallId(callId).map(this::toCallResponse);
    }

    public List<CallStatusHistoryResponse> getCallHistory(String callId) {
        return callRepository.findHistory(callId).stream()
                .map(entry -> new CallStatusHistoryResponse(entry.status(), entry.changedAt()))
                .collect(Collectors.toList());
    }

    public CallProjectionPageResponse listCalls(String status, String priority, String callType, int page, int size) {
        List<CallProjectionResponse> content = callRepository.findAll(status, priority, callType, page, size)
                .stream()
                .map(this::toCallResponse)
                .collect(Collectors.toList());
        long total = callRepository.count(status, priority, callType);
        return new CallProjectionPageResponse(content, total, page, size);
    }

    private CallProjectionResponse toCallResponse(CallProjectionEntity entity) {
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
                entity.incidentId(),
                entity.updatedAt()
        );
    }

    // Query methods - Dispatches
    public Optional<DispatchProjectionResponse> getDispatch(String dispatchId) {
        return dispatchRepository.findByDispatchId(dispatchId).map(this::toDispatchResponse);
    }

    public List<DispatchStatusHistoryResponse> getDispatchHistory(String dispatchId) {
        return dispatchRepository.findHistory(dispatchId).stream()
                .map(entry -> new DispatchStatusHistoryResponse(entry.status(), entry.changedAt()))
                .collect(Collectors.toList());
    }

    public DispatchProjectionPageResponse listDispatches(String status, String dispatchType, int page, int size) {
        List<DispatchProjectionResponse> content = dispatchRepository.findAll(status, dispatchType, page, size)
                .stream()
                .map(this::toDispatchResponse)
                .collect(Collectors.toList());
        long total = dispatchRepository.count(status, dispatchType);
        return new DispatchProjectionPageResponse(content, total, page, size);
    }

    private DispatchProjectionResponse toDispatchResponse(DispatchProjectionEntity entity) {
        return new DispatchProjectionResponse(
                entity.dispatchId(),
                entity.dispatchTime(),
                entity.dispatchType(),
                entity.status(),
                entity.callId(),
                entity.updatedAt()
        );
    }

    // Query methods - Activities
    public Optional<ActivityProjectionResponse> getActivity(String activityId) {
        return activityRepository.findByActivityId(activityId).map(this::toActivityResponse);
    }

    public List<ActivityStatusHistoryResponse> getActivityHistory(String activityId) {
        return activityRepository.findHistory(activityId).stream()
                .map(entry -> new ActivityStatusHistoryResponse(entry.status(), entry.changedAt()))
                .collect(Collectors.toList());
    }

    public ActivityProjectionPageResponse listActivities(String status, String activityType, int page, int size) {
        List<ActivityProjectionResponse> content = activityRepository.findAll(status, activityType, page, size)
                .stream()
                .map(this::toActivityResponse)
                .collect(Collectors.toList());
        long total = activityRepository.count(status, activityType);
        return new ActivityProjectionPageResponse(content, total, page, size);
    }

    private ActivityProjectionResponse toActivityResponse(ActivityProjectionEntity entity) {
        return new ActivityProjectionResponse(
                entity.activityId(),
                entity.activityTime(),
                entity.activityType(),
                entity.description(),
                entity.status(),
                entity.completedTime(),
                entity.incidentId(),
                entity.updatedAt()
        );
    }

    // Query methods - Assignments
    public Optional<AssignmentProjectionResponse> getAssignment(String assignmentId) {
        return assignmentRepository.findByAssignmentId(assignmentId).map(this::toAssignmentResponse);
    }

    public List<AssignmentStatusHistoryResponse> getAssignmentHistory(String assignmentId) {
        return assignmentRepository.findHistory(assignmentId).stream()
                .map(entry -> new AssignmentStatusHistoryResponse(entry.status(), entry.changedAt()))
                .collect(Collectors.toList());
    }

    public AssignmentProjectionPageResponse listAssignments(String status, String assignmentType, String dispatchId,
                                                             String incidentId, String callId, int page, int size) {
        List<AssignmentProjectionResponse> content = assignmentRepository.findAll(status, assignmentType, dispatchId, incidentId, callId, page, size)
                .stream()
                .map(this::toAssignmentResponse)
                .collect(Collectors.toList());
        long total = assignmentRepository.count(status, assignmentType, dispatchId, incidentId, callId);
        return new AssignmentProjectionPageResponse(content, total, page, size);
    }

    private AssignmentProjectionResponse toAssignmentResponse(AssignmentProjectionEntity entity) {
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

    // Query methods - Involved Parties
    public Optional<InvolvedPartyProjectionResponse> getInvolvedParty(String involvementId) {
        return involvedPartyRepository.findByInvolvementId(involvementId).map(this::toInvolvedPartyResponse);
    }

    public InvolvedPartyProjectionPageResponse listInvolvedParties(String personId, String partyRoleType, String incidentId,
                                                                   String callId, String activityId, int page, int size) {
        List<InvolvedPartyProjectionResponse> content = involvedPartyRepository.findAll(personId, partyRoleType, incidentId, callId, activityId, page, size)
                .stream()
                .map(this::toInvolvedPartyResponse)
                .collect(Collectors.toList());
        long total = involvedPartyRepository.count(personId, partyRoleType, incidentId, callId, activityId);
        return new InvolvedPartyProjectionPageResponse(content, total, page, size);
    }

    private InvolvedPartyProjectionResponse toInvolvedPartyResponse(InvolvedPartyProjectionEntity entity) {
        return new InvolvedPartyProjectionResponse(
                entity.involvementId(),
                entity.personId(),
                entity.partyRoleType(),
                entity.description(),
                entity.involvementStartTime(),
                entity.involvementEndTime(),
                entity.incidentId(),
                entity.callId(),
                entity.activityId(),
                entity.updatedAt()
        );
    }

    // Query methods - Resource Assignments
    public Optional<ResourceAssignmentProjectionResponse> getResourceAssignment(Long id) {
        return resourceAssignmentRepository.findById(id).map(this::toResourceAssignmentResponse);
    }

    public ResourceAssignmentProjectionPageResponse listResourceAssignments(String assignmentId, String resourceId,
                                                                             String resourceType, String roleType, String status,
                                                                             int page, int size) {
        List<ResourceAssignmentProjectionResponse> content = resourceAssignmentRepository.findAll(assignmentId, resourceId, resourceType, roleType, status, page, size)
                .stream()
                .map(this::toResourceAssignmentResponse)
                .collect(Collectors.toList());
        long total = resourceAssignmentRepository.count(assignmentId, resourceId, resourceType, roleType, status);
        return new ResourceAssignmentProjectionPageResponse(content, total, page, size);
    }

    private ResourceAssignmentProjectionResponse toResourceAssignmentResponse(ResourceAssignmentProjectionEntity entity) {
        return new ResourceAssignmentProjectionResponse(
                entity.id(),
                entity.assignmentId(),
                entity.resourceId(),
                entity.resourceType(),
                entity.roleType(),
                entity.status(),
                entity.startTime(),
                entity.endTime(),
                entity.updatedAt()
        );
    }

    // Composite query methods
    public Optional<IncidentFullResponse> getIncidentFull(String incidentId) {
        Optional<IncidentProjectionResponse> incidentOpt = getIncident(incidentId);
        if (incidentOpt.isEmpty()) {
            return Optional.empty();
        }

        IncidentProjectionResponse incident = incidentOpt.get();

        // Get calls for the incident
        List<CallProjectionEntity> callEntities = callRepository.findByIncidentId(incidentId);
        List<CallProjectionResponse> calls = callEntities.stream()
                .map(this::toCallResponse)
                .collect(Collectors.toList());

        // Get dispatches for each call
        List<CallWithDispatchesResponse> callsWithDispatches = calls.stream()
                .map(call -> {
                    List<DispatchProjectionEntity> dispatchEntities = dispatchRepository.findByCallId(call.callId());
                    List<DispatchProjectionResponse> dispatches = dispatchEntities.stream()
                            .map(this::toDispatchResponse)
                            .collect(Collectors.toList());
                    return new CallWithDispatchesResponse(call, dispatches);
                })
                .collect(Collectors.toList());

        // Get activities for the incident
        List<ActivityProjectionEntity> activityEntities = activityRepository.findByIncidentId(incidentId);
        List<ActivityProjectionResponse> activities = activityEntities.stream()
                .map(this::toActivityResponse)
                .collect(Collectors.toList());

        // Get assignments (for incident and related calls/dispatches)
        Set<String> callIds = calls.stream().map(CallProjectionResponse::callId).collect(Collectors.toSet());
        Set<String> dispatchIds = callsWithDispatches.stream()
                .flatMap(c -> c.dispatches().stream().map(DispatchProjectionResponse::dispatchId))
                .collect(Collectors.toSet());

        List<AssignmentProjectionEntity> assignmentEntities = new ArrayList<>();
        assignmentEntities.addAll(assignmentRepository.findByIncidentId(incidentId));
        callIds.forEach(callId -> assignmentEntities.addAll(assignmentRepository.findByCallId(callId)));
        dispatchIds.forEach(dispatchId -> assignmentEntities.addAll(assignmentRepository.findByDispatchId(dispatchId)));

        List<AssignmentProjectionResponse> assignments = assignmentEntities.stream()
                .map(this::toAssignmentResponse)
                .collect(Collectors.toList());

        // Get resource assignments for each assignment
        List<AssignmentWithResourcesResponse> assignmentsWithResources = assignments.stream()
                .map(assignment -> {
                    List<ResourceAssignmentProjectionEntity> resourceEntities = resourceAssignmentRepository
                            .findByAssignmentId(assignment.assignmentId());
                    List<ResourceAssignmentProjectionResponse> resources = resourceEntities.stream()
                            .map(this::toResourceAssignmentResponse)
                            .collect(Collectors.toList());
                    return new AssignmentWithResourcesResponse(assignment, resources);
                })
                .collect(Collectors.toList());

        // Get involved parties (for incident, calls, and activities)
        List<InvolvedPartyProjectionEntity> involvedPartyEntities = new ArrayList<>();
        involvedPartyEntities.addAll(involvedPartyRepository.findByIncidentId(incidentId));
        callIds.forEach(callId -> involvedPartyEntities.addAll(involvedPartyRepository.findByCallId(callId)));
        activities.stream().map(ActivityProjectionResponse::activityId).forEach(activityId ->
                involvedPartyEntities.addAll(involvedPartyRepository.findByActivityId(activityId)));

        List<InvolvedPartyProjectionResponse> involvedParties = involvedPartyEntities.stream()
                .map(this::toInvolvedPartyResponse)
                .collect(Collectors.toList());

        return Optional.of(new IncidentFullResponse(
                incident,
                callsWithDispatches,
                activities,
                assignmentsWithResources,
                involvedParties
        ));
    }

    private Instant now() {
        return clock.instant();
    }
}
