package com.knowit.policesystem.projection.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationalEventParserTest {

    private OperationalEventParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        parser = new OperationalEventParser(objectMapper);
    }

    @Test
    void parse_WithReportIncidentRequested_ReturnsCorrectEvent() throws Exception {
        ReportIncidentRequested event = new ReportIncidentRequested(
                "INC-001", "2024-001", "High", "Reported", Instant.now(), "Test incident", "Traffic"
        );
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(ReportIncidentRequested.class);
        ReportIncidentRequested parsed = (ReportIncidentRequested) result;
        assertThat(parsed.getIncidentId()).isEqualTo("INC-001");
        assertThat(parsed.getIncidentNumber()).isEqualTo("2024-001");
    }

    @Test
    void parse_WithReceiveCallRequested_ReturnsCorrectEvent() throws Exception {
        ReceiveCallRequested event = new ReceiveCallRequested(
                "CALL-001", "CALL-001", "High", "Received", Instant.now(), "Test call", "Emergency"
        );
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(ReceiveCallRequested.class);
        ReceiveCallRequested parsed = (ReceiveCallRequested) result;
        assertThat(parsed.getCallId()).isEqualTo("CALL-001");
    }

    @Test
    void parse_WithCreateDispatchRequested_ReturnsCorrectEvent() throws Exception {
        CreateDispatchRequested event = new CreateDispatchRequested(
                "DISP-001", Instant.now(), "Initial", "Dispatched"
        );
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(CreateDispatchRequested.class);
        CreateDispatchRequested parsed = (CreateDispatchRequested) result;
        assertThat(parsed.getDispatchId()).isEqualTo("DISP-001");
    }

    @Test
    void parse_WithStartActivityRequested_ReturnsCorrectEvent() throws Exception {
        StartActivityRequested event = new StartActivityRequested(
                "ACT-001", Instant.now(), "Investigation", "Test activity", "InProgress"
        );
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(StartActivityRequested.class);
        StartActivityRequested parsed = (StartActivityRequested) result;
        assertThat(parsed.getActivityId()).isEqualTo("ACT-001");
    }

    @Test
    void parse_WithCreateAssignmentRequested_ReturnsCorrectEvent() throws Exception {
        CreateAssignmentRequested event = new CreateAssignmentRequested(
                "ASSIGN-001", "ASSIGN-001", Instant.now(), "Primary", "Assigned", "INC-001", null
        );
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(CreateAssignmentRequested.class);
        CreateAssignmentRequested parsed = (CreateAssignmentRequested) result;
        assertThat(parsed.getAssignmentId()).isEqualTo("ASSIGN-001");
    }

    @Test
    void parse_WithInvolvePartyRequested_ReturnsCorrectEvent() throws Exception {
        InvolvePartyRequested event = new InvolvePartyRequested(
                "INV-001", "INV-001", "PERSON-001", "INC-001", null, null, "Victim", "Test description", Instant.now()
        );
        String payload = objectMapper.writeValueAsString(event);
        // Add eventType since it's not serialized by default
        String payloadWithEventType = payload.replaceFirst("\\{", "{\"eventType\":\"InvolvePartyRequested\",");

        Object result = parser.parse(payloadWithEventType, null);

        assertThat(result).isInstanceOf(InvolvePartyRequested.class);
        InvolvePartyRequested parsed = (InvolvePartyRequested) result;
        assertThat(parsed.getInvolvementId()).isEqualTo("INV-001");
    }

    @Test
    void parse_WithAssignResourceRequested_ReturnsCorrectEvent() throws Exception {
        AssignResourceRequested event = new AssignResourceRequested(
                "ASSIGN-001", "ASSIGN-001", "OFFICER-001", "Officer", "Primary", "Assigned", Instant.now()
        );
        String payload = objectMapper.writeValueAsString(event);
        // Add eventType since it's not serialized by default
        String payloadWithEventType = payload.replaceFirst("\\{", "{\"eventType\":\"AssignResourceRequested\",");

        Object result = parser.parse(payloadWithEventType, null);

        assertThat(result).isInstanceOf(AssignResourceRequested.class);
        AssignResourceRequested parsed = (AssignResourceRequested) result;
        assertThat(parsed.getAssignmentId()).isEqualTo("ASSIGN-001");
    }

    @Test
    void parse_WithEventTypeField_UsesEventType() throws Exception {
        String payload = """
                {
                    "eventType": "ChangeIncidentStatusRequested",
                    "incidentId": "INC-001",
                    "status": "Active"
                }
                """;

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(ChangeIncidentStatusRequested.class);
    }

    @Test
    void parse_WithInvalidPayload_ThrowsException() {
        String payload = "invalid json";

        assertThatThrownBy(() -> parser.parse(payload, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to parse operational event");
    }

    @Test
    void parse_WithUnknownEventType_FallsBackToHeuristics() throws Exception {
        String payload = """
                {
                    "eventType": "UnknownEventType",
                    "incidentId": "INC-001",
                    "reportedTime": "2024-01-01T10:00:00Z",
                    "incidentNumber": "2024-001"
                }
                """;

        Object result = parser.parse(payload, null);

        // Should fall back to heuristics and parse as ReportIncidentRequested
        assertThat(result).isInstanceOf(ReportIncidentRequested.class);
    }

    @Test
    void parse_WithSubjectHint_UsesHintForDomainDetection() throws Exception {
        String payload = """
                {
                    "incidentId": "INC-001",
                    "status": "Active"
                }
                """;

        Object result = parser.parse(payload, "commands.incident.change-status");

        assertThat(result).isInstanceOf(ChangeIncidentStatusRequested.class);
    }

    // Test all event types with eventType field
    @Test
    void parse_AllIncidentEventTypes_WithEventType() throws Exception {
        testEventType("ReportIncidentRequested", ReportIncidentRequested.class);
        testEventType("UpdateIncidentRequested", UpdateIncidentRequested.class);
        testEventType("ChangeIncidentStatusRequested", ChangeIncidentStatusRequested.class);
        testEventType("DispatchIncidentRequested", DispatchIncidentRequested.class);
        testEventType("ArriveAtIncidentRequested", ArriveAtIncidentRequested.class);
        testEventType("ClearIncidentRequested", ClearIncidentRequested.class);
    }

    @Test
    void parse_AllCallEventTypes_WithEventType() throws Exception {
        testEventType("ReceiveCallRequested", ReceiveCallRequested.class);
        testEventType("UpdateCallRequested", UpdateCallRequested.class);
        testEventType("ChangeCallStatusRequested", ChangeCallStatusRequested.class);
        testEventType("DispatchCallRequested", DispatchCallRequested.class);
        testEventType("ArriveAtCallRequested", ArriveAtCallRequested.class);
        testEventType("ClearCallRequested", ClearCallRequested.class);
        testEventType("LinkCallToIncidentRequested", LinkCallToIncidentRequested.class);
        testEventType("LinkCallToDispatchRequested", LinkCallToDispatchRequested.class);
    }

    @Test
    void parse_AllDispatchEventTypes_WithEventType() throws Exception {
        testEventType("CreateDispatchRequested", CreateDispatchRequested.class);
        testEventType("ChangeDispatchStatusRequested", ChangeDispatchStatusRequested.class);
    }

    @Test
    void parse_AllActivityEventTypes_WithEventType() throws Exception {
        testEventType("StartActivityRequested", StartActivityRequested.class);
        testEventType("UpdateActivityRequested", UpdateActivityRequested.class);
        testEventType("ChangeActivityStatusRequested", ChangeActivityStatusRequested.class);
        testEventType("CompleteActivityRequested", CompleteActivityRequested.class);
        testEventType("LinkActivityToIncidentRequested", LinkActivityToIncidentRequested.class);
    }

    @Test
    void parse_AllAssignmentEventTypes_WithEventType() throws Exception {
        testEventType("CreateAssignmentRequested", CreateAssignmentRequested.class);
        testEventType("ChangeAssignmentStatusRequested", ChangeAssignmentStatusRequested.class);
        testEventType("CompleteAssignmentRequested", CompleteAssignmentRequested.class);
        testEventType("LinkAssignmentToDispatchRequested", LinkAssignmentToDispatchRequested.class);
    }

    @Test
    void parse_AllInvolvedPartyEventTypes_WithEventType() throws Exception {
        testEventType("InvolvePartyRequested", InvolvePartyRequested.class);
        testEventType("UpdatePartyInvolvementRequested", UpdatePartyInvolvementRequested.class);
        testEventType("EndPartyInvolvementRequested", EndPartyInvolvementRequested.class);
    }

    @Test
    void parse_AllResourceAssignmentEventTypes_WithEventType() throws Exception {
        testEventType("AssignResourceRequested", AssignResourceRequested.class);
        testEventType("UnassignResourceRequested", UnassignResourceRequested.class);
        testEventType("ChangeResourceAssignmentStatusRequested", ChangeResourceAssignmentStatusRequested.class);
    }

    private void testEventType(String eventTypeName, Class<?> expectedClass) throws Exception {
        String payload = String.format("""
                {
                    "eventType": "%s",
                    "eventId": "%s",
                    "timestamp": "2024-01-01T10:00:00Z",
                    "aggregateId": "TEST-001"
                }
                """, eventTypeName, UUID.randomUUID());

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(expectedClass);
    }
}
