package com.knowit.policesystem.projection.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.nats.query.ExistsQueryRequest;
import com.knowit.policesystem.common.nats.query.ExistsQueryResponse;
import com.knowit.policesystem.common.nats.query.GetQueryRequest;
import com.knowit.policesystem.common.nats.query.GetQueryResponse;
import com.knowit.policesystem.projection.api.CallProjectionResponse;
import com.knowit.policesystem.projection.api.IncidentProjectionResponse;
import com.knowit.policesystem.projection.api.ResourceAssignmentProjectionResponse;
import com.knowit.policesystem.projection.config.NatsProperties;
import com.knowit.policesystem.projection.service.OperationalProjectionService;
import io.nats.client.Connection;
import io.nats.client.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationalNatsQueryHandlerTest {

    @Mock
    private NatsProperties properties;

    @Mock
    private OperationalProjectionService projectionService;

    @Mock
    private Connection connection;

    private ObjectMapper objectMapper;
    private OperationalNatsQueryHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        handler = new OperationalNatsQueryHandler(properties, projectionService, objectMapper);
    }

    @Test
    void start_WhenQueryDisabled_DoesNotConnect() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isQueryEnabled()).thenReturn(false);

        handler.start();

        // Should not throw exception when disabled
    }

    @Test
    void handleExistsQuery_ForIncident_WhenExists_ReturnsTrue() throws Exception {
        String queryId = "query-123";
        String incidentId = "INC-001";
        IncidentProjectionResponse incident = new IncidentProjectionResponse(
                incidentId, "2024-001", "High", "Reported", Instant.now(), null, null, null, "Test", "Traffic", Instant.now()
        );

        when(projectionService.getIncident(incidentId)).thenReturn(Optional.of(incident));
        doNothing().when(connection).publish(anyString(), any(byte[].class));

        Message message = createMessageWithReply("query.incident.exists", createExistsRequestJson(queryId, incidentId));
        injectConnection(handler);
        reset(connection); // Reset to clear any interactions from injection

        Method handleExistsQuery = OperationalNatsQueryHandler.class.getDeclaredMethod(
                "handleExistsQuery", Message.class, String.class, String.class);
        handleExistsQuery.setAccessible(true);
        handleExistsQuery.invoke(handler, message, createExistsRequestJson(queryId, incidentId), "query.incident.exists");

        ArgumentCaptor<byte[]> responseCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(connection, atLeastOnce()).publish(anyString(), responseCaptor.capture());

        String responseJson = new String(responseCaptor.getValue(), StandardCharsets.UTF_8);
        ExistsQueryResponse response = objectMapper.readValue(responseJson, ExistsQueryResponse.class);
        assertThat(response.isExists()).isTrue();
        assertThat(response.getQueryId()).isEqualTo(queryId);
    }

    @Test
    void handleExistsQuery_ForIncident_WhenNotExists_ReturnsFalse() throws Exception {
        String queryId = "query-123";
        String incidentId = "INC-999";

        when(projectionService.getIncident(incidentId)).thenReturn(Optional.empty());

        Message message = createMessageWithReply("query.incident.exists", createExistsRequestJson(queryId, incidentId));
        injectConnection(handler);
        reset(connection); // Reset to clear any interactions from injection
        doNothing().when(connection).publish(anyString(), any(byte[].class));

        Method handleExistsQuery = OperationalNatsQueryHandler.class.getDeclaredMethod(
                "handleExistsQuery", Message.class, String.class, String.class);
        handleExistsQuery.setAccessible(true);
        handleExistsQuery.invoke(handler, message, createExistsRequestJson(queryId, incidentId), "query.incident.exists");

        ArgumentCaptor<byte[]> responseCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(connection, atLeastOnce()).publish(anyString(), responseCaptor.capture());

        String responseJson = new String(responseCaptor.getValue(), StandardCharsets.UTF_8);
        ExistsQueryResponse response = objectMapper.readValue(responseJson, ExistsQueryResponse.class);
        assertThat(response.isExists()).isFalse();
    }

    @Test
    void handleGetQuery_ForCall_WhenFound_ReturnsData() throws Exception {
        String queryId = "query-123";
        String callId = "CALL-001";
        CallProjectionResponse call = new CallProjectionResponse(
                callId, "CALL-001", "High", "Received", Instant.now(), null, null, null, "Test call", "Emergency", "INC-001", Instant.now()
        );

        when(projectionService.getCall(callId)).thenReturn(Optional.of(call));

        Message message = createMessageWithReply("query.call.get", createGetRequestJson(queryId, callId));
        injectConnection(handler);
        reset(connection); // Reset to clear any interactions from injection
        doNothing().when(connection).publish(anyString(), any(byte[].class));

        Method handleGetQuery = OperationalNatsQueryHandler.class.getDeclaredMethod(
                "handleGetQuery", Message.class, String.class, String.class);
        handleGetQuery.setAccessible(true);
        handleGetQuery.invoke(handler, message, createGetRequestJson(queryId, callId), "query.call.get");

        ArgumentCaptor<byte[]> responseCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(connection, atLeastOnce()).publish(anyString(), responseCaptor.capture());

        String responseJson = new String(responseCaptor.getValue(), StandardCharsets.UTF_8);
        GetQueryResponse response = objectMapper.readValue(responseJson, GetQueryResponse.class);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getQueryId()).isEqualTo(queryId);
    }

    @Test
    void handleGetQuery_ForResourceAssignment_WithInvalidId_HandlesGracefully() throws Exception {
        String queryId = "query-123";
        String invalidId = "not-a-number";

        Message message = createMessageWithReply("query.resource-assignment.get", createGetRequestJson(queryId, invalidId));
        injectConnection(handler);
        reset(connection); // Reset to clear any interactions from injection
        doNothing().when(connection).publish(anyString(), any(byte[].class));

        Method handleGetQuery = OperationalNatsQueryHandler.class.getDeclaredMethod(
                "handleGetQuery", Message.class, String.class, String.class);
        handleGetQuery.setAccessible(true);
        handleGetQuery.invoke(handler, message, createGetRequestJson(queryId, invalidId), "query.resource-assignment.get");

        ArgumentCaptor<byte[]> responseCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(connection, atLeastOnce()).publish(anyString(), responseCaptor.capture());

        String responseJson = new String(responseCaptor.getValue(), StandardCharsets.UTF_8);
        GetQueryResponse response = objectMapper.readValue(responseJson, GetQueryResponse.class);
        assertThat(response.getData()).isNull();
    }

    @Test
    void handleQuery_WithUnknownOperation_SendsError() throws Exception {
        Message message = createMessageWithReply("query.incident.unknown", "{}");
        injectConnection(handler);
        reset(connection); // Reset to clear any interactions from injection
        doNothing().when(connection).publish(anyString(), any(byte[].class));

        Method handleQuery = OperationalNatsQueryHandler.class.getDeclaredMethod("handleQuery", Message.class);
        handleQuery.setAccessible(true);
        handleQuery.invoke(handler, message);

        ArgumentCaptor<byte[]> responseCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(connection, atLeastOnce()).publish(anyString(), responseCaptor.capture());

        String responseJson = new String(responseCaptor.getValue(), StandardCharsets.UTF_8);
        GetQueryResponse response = objectMapper.readValue(responseJson, GetQueryResponse.class);
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("Unknown query operation");
    }

    // Helper methods

    private Message createMessageWithReply(String subject, String payload) {
        Message message = mock(Message.class);
        lenient().when(message.getSubject()).thenReturn(subject);
        lenient().when(message.getData()).thenReturn(payload.getBytes(StandardCharsets.UTF_8));
        lenient().when(message.getReplyTo()).thenReturn("reply.subject");
        return message;
    }

    private String createExistsRequestJson(String queryId, String resourceId) {
        try {
            ExistsQueryRequest request = new ExistsQueryRequest(queryId, "operational", resourceId);
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String createGetRequestJson(String queryId, String resourceId) {
        try {
            GetQueryRequest request = new GetQueryRequest(queryId, "operational", resourceId);
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void injectConnection(OperationalNatsQueryHandler handler) throws Exception {
        Field connectionField = OperationalNatsQueryHandler.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(handler, connection);
    }
}
