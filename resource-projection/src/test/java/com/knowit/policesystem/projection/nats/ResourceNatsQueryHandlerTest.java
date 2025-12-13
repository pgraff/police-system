package com.knowit.policesystem.projection.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.nats.query.ExistsQueryRequest;
import com.knowit.policesystem.common.nats.query.ExistsQueryResponse;
import com.knowit.policesystem.common.nats.query.GetQueryRequest;
import com.knowit.policesystem.common.nats.query.GetQueryResponse;
import com.knowit.policesystem.projection.api.OfficerProjectionResponse;
import com.knowit.policesystem.projection.config.NatsProperties;
import com.knowit.policesystem.projection.service.ResourceProjectionService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceNatsQueryHandlerTest {

    @Mock
    private NatsProperties properties;

    @Mock
    private ResourceProjectionService projectionService;

    @Mock
    private Connection connection;

    private ObjectMapper objectMapper;
    private ResourceNatsQueryHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        handler = new ResourceNatsQueryHandler(properties, projectionService, objectMapper);
    }

    @Test
    void start_WhenQueryDisabled_DoesNotConnect() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isQueryEnabled()).thenReturn(false);

        handler.start();

        // Should not throw exception when disabled
    }

    @Test
    void handleExistsQuery_ForOfficer_WhenExists_ReturnsTrue() throws Exception {
        String queryId = "query-123";
        String badgeNumber = "BADGE-001";
        OfficerProjectionResponse officer = new OfficerProjectionResponse(
                badgeNumber, "John", "Doe", "Sergeant", "john.doe@police.gov",
                "555-0100", "2020-01-15", "Active", Instant.now()
        );

        when(projectionService.getOfficer(badgeNumber)).thenReturn(Optional.of(officer));
        doNothing().when(connection).publish(anyString(), any(byte[].class));

        Message message = createMessageWithReply("query.officer.exists", createExistsRequestJson(queryId, badgeNumber));
        injectConnection(handler);
        reset(connection);

        Method handleExistsQuery = ResourceNatsQueryHandler.class.getDeclaredMethod(
                "handleExistsQuery", Message.class, String.class, String.class);
        handleExistsQuery.setAccessible(true);
        handleExistsQuery.invoke(handler, message, createExistsRequestJson(queryId, badgeNumber), "query.officer.exists");

        ArgumentCaptor<byte[]> responseCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(connection, atLeastOnce()).publish(anyString(), responseCaptor.capture());

        String responseJson = new String(responseCaptor.getValue(), StandardCharsets.UTF_8);
        ExistsQueryResponse response = objectMapper.readValue(responseJson, ExistsQueryResponse.class);
        assertThat(response.isExists()).isTrue();
        assertThat(response.getQueryId()).isEqualTo(queryId);
    }

    @Test
    void handleExistsQuery_ForOfficer_WhenNotExists_ReturnsFalse() throws Exception {
        String queryId = "query-123";
        String badgeNumber = "BADGE-999";

        when(projectionService.getOfficer(badgeNumber)).thenReturn(Optional.empty());
        doNothing().when(connection).publish(anyString(), any(byte[].class));

        Message message = createMessageWithReply("query.officer.exists", createExistsRequestJson(queryId, badgeNumber));
        injectConnection(handler);
        reset(connection);

        Method handleExistsQuery = ResourceNatsQueryHandler.class.getDeclaredMethod(
                "handleExistsQuery", Message.class, String.class, String.class);
        handleExistsQuery.setAccessible(true);
        handleExistsQuery.invoke(handler, message, createExistsRequestJson(queryId, badgeNumber), "query.officer.exists");

        ArgumentCaptor<byte[]> responseCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(connection, atLeastOnce()).publish(anyString(), responseCaptor.capture());

        String responseJson = new String(responseCaptor.getValue(), StandardCharsets.UTF_8);
        ExistsQueryResponse response = objectMapper.readValue(responseJson, ExistsQueryResponse.class);
        assertThat(response.isExists()).isFalse();
    }

    @Test
    void handleGetQuery_ForOfficer_WhenExists_ReturnsData() throws Exception {
        String queryId = "query-123";
        String badgeNumber = "BADGE-001";
        OfficerProjectionResponse officer = new OfficerProjectionResponse(
                badgeNumber, "John", "Doe", "Sergeant", "john.doe@police.gov",
                "555-0100", "2020-01-15", "Active", Instant.now()
        );

        when(projectionService.getOfficer(badgeNumber)).thenReturn(Optional.of(officer));
        doNothing().when(connection).publish(anyString(), any(byte[].class));

        Message message = createMessageWithReply("query.officer.get", createGetRequestJson(queryId, badgeNumber));
        injectConnection(handler);
        reset(connection);

        Method handleGetQuery = ResourceNatsQueryHandler.class.getDeclaredMethod(
                "handleGetQuery", Message.class, String.class, String.class);
        handleGetQuery.setAccessible(true);
        handleGetQuery.invoke(handler, message, createGetRequestJson(queryId, badgeNumber), "query.officer.get");

        ArgumentCaptor<byte[]> responseCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(connection, atLeastOnce()).publish(anyString(), responseCaptor.capture());

        String responseJson = new String(responseCaptor.getValue(), StandardCharsets.UTF_8);
        GetQueryResponse response = objectMapper.readValue(responseJson, GetQueryResponse.class);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getQueryId()).isEqualTo(queryId);
    }

    private Message createMessageWithReply(String subject, String payload) {
        Message message = mock(Message.class);
        lenient().when(message.getSubject()).thenReturn(subject);
        lenient().when(message.getData()).thenReturn(payload.getBytes(StandardCharsets.UTF_8));
        lenient().when(message.getReplyTo()).thenReturn("reply.subject");
        return message;
    }

    private String createExistsRequestJson(String queryId, String resourceId) {
        try {
            ExistsQueryRequest request = new ExistsQueryRequest(queryId, "resource", resourceId);
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String createGetRequestJson(String queryId, String resourceId) {
        try {
            GetQueryRequest request = new GetQueryRequest(queryId, "resource", resourceId);
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void injectConnection(ResourceNatsQueryHandler handler) throws Exception {
        Field connectionField = ResourceNatsQueryHandler.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(handler, connection);
    }
}
