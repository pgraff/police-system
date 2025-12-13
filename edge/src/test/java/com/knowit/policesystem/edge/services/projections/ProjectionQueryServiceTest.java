package com.knowit.policesystem.edge.services.projections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.nats.NatsQueryClient;
import com.knowit.policesystem.common.nats.query.ExistsQueryRequest;
import com.knowit.policesystem.common.nats.query.ExistsQueryResponse;
import com.knowit.policesystem.common.nats.query.GetQueryRequest;
import com.knowit.policesystem.common.nats.query.GetQueryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for ProjectionQueryService.
 */
@ExtendWith(MockitoExtension.class)
class ProjectionQueryServiceTest {

    @Mock
    private NatsQueryClient queryClient;

    private ProjectionQueryService projectionQueryService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        projectionQueryService = new ProjectionQueryService(queryClient);
    }

    @Test
    void testExists_ResourceExists_ReturnsTrue() throws Exception {
        // Given
        String domain = "officer";
        String resourceId = "BADGE-123";
        ExistsQueryResponse response = new ExistsQueryResponse("query-id", true);
        when(queryClient.query(eq("query.officer.exists"), any(ExistsQueryRequest.class), eq(ExistsQueryResponse.class)))
                .thenReturn(response);

        // When
        boolean exists = projectionQueryService.exists(domain, resourceId);

        // Then
        assertThat(exists).isTrue();
        verify(queryClient).query(eq("query.officer.exists"), any(ExistsQueryRequest.class), eq(ExistsQueryResponse.class));
    }

    @Test
    void testExists_ResourceNotExists_ReturnsFalse() throws Exception {
        // Given
        String domain = "call";
        String resourceId = "CALL-456";
        ExistsQueryResponse response = new ExistsQueryResponse("query-id", false);
        when(queryClient.query(eq("query.call.exists"), any(ExistsQueryRequest.class), eq(ExistsQueryResponse.class)))
                .thenReturn(response);

        // When
        boolean exists = projectionQueryService.exists(domain, resourceId);

        // Then
        assertThat(exists).isFalse();
        verify(queryClient).query(eq("query.call.exists"), any(ExistsQueryRequest.class), eq(ExistsQueryResponse.class));
    }

    @Test
    void testExists_QueryFails_ThrowsProjectionQueryException() throws Exception {
        // Given
        String domain = "incident";
        String resourceId = "INC-789";
        NatsQueryClient.NatsQueryException natsException = new NatsQueryClient.NatsQueryException("Connection failed");
        when(queryClient.query(anyString(), any(ExistsQueryRequest.class), eq(ExistsQueryResponse.class)))
                .thenThrow(natsException);

        // When/Then
        assertThatThrownBy(() -> projectionQueryService.exists(domain, resourceId))
                .isInstanceOf(ProjectionQueryService.ProjectionQueryException.class)
                .hasMessageContaining("Failed to query projection for existence")
                .hasCause(natsException);
    }

    @Test
    void testExists_CorrectSubject_Used() throws Exception {
        // Given
        String domain = "activity";
        String resourceId = "ACT-123";
        ExistsQueryResponse response = new ExistsQueryResponse("query-id", true);
        when(queryClient.query(anyString(), any(ExistsQueryRequest.class), eq(ExistsQueryResponse.class)))
                .thenReturn(response);

        // When
        projectionQueryService.exists(domain, resourceId);

        // Then - verify correct subject pattern
        verify(queryClient).query(eq("query.activity.exists"), any(ExistsQueryRequest.class), eq(ExistsQueryResponse.class));
    }

    @Test
    void testGet_ResourceExists_ReturnsData() throws Exception {
        // Given
        String domain = "officer";
        String resourceId = "BADGE-123";
        Map<String, Object> data = Map.of("badgeNumber", "BADGE-123", "firstName", "John", "lastName", "Doe");
        GetQueryResponse response = new GetQueryResponse("query-id", data);
        when(queryClient.query(eq("query.officer.get"), any(GetQueryRequest.class), eq(GetQueryResponse.class)))
                .thenReturn(response);

        // When
        Object result = projectionQueryService.get(domain, resourceId);

        // Then
        assertThat(result).isEqualTo(data);
        verify(queryClient).query(eq("query.officer.get"), any(GetQueryRequest.class), eq(GetQueryResponse.class));
    }

    @Test
    void testGet_ResourceNotExists_ReturnsNull() throws Exception {
        // Given
        String domain = "call";
        String resourceId = "CALL-456";
        GetQueryResponse response = new GetQueryResponse("query-id", null);
        when(queryClient.query(eq("query.call.get"), any(GetQueryRequest.class), eq(GetQueryResponse.class)))
                .thenReturn(response);

        // When
        Object result = projectionQueryService.get(domain, resourceId);

        // Then
        assertThat(result).isNull();
        verify(queryClient).query(eq("query.call.get"), any(GetQueryRequest.class), eq(GetQueryResponse.class));
    }

    @Test
    void testGet_QueryFails_ThrowsProjectionQueryException() throws Exception {
        // Given
        String domain = "incident";
        String resourceId = "INC-789";
        NatsQueryClient.NatsQueryException natsException = new NatsQueryClient.NatsQueryException("Timeout");
        when(queryClient.query(anyString(), any(GetQueryRequest.class), eq(GetQueryResponse.class)))
                .thenThrow(natsException);

        // When/Then
        assertThatThrownBy(() -> projectionQueryService.get(domain, resourceId))
                .isInstanceOf(ProjectionQueryService.ProjectionQueryException.class)
                .hasMessageContaining("Failed to query projection for resource")
                .hasCause(natsException);
    }

    @Test
    void testGet_CorrectSubject_Used() throws Exception {
        // Given
        String domain = "dispatch";
        String resourceId = "DISP-123";
        GetQueryResponse response = new GetQueryResponse("query-id", null);
        when(queryClient.query(anyString(), any(GetQueryRequest.class), eq(GetQueryResponse.class)))
                .thenReturn(response);

        // When
        projectionQueryService.get(domain, resourceId);

        // Then - verify correct subject pattern
        verify(queryClient).query(eq("query.dispatch.get"), any(GetQueryRequest.class), eq(GetQueryResponse.class));
    }

    @Test
    void testExists_WithNullQueryClient_ThrowsException() {
        // Given
        ProjectionQueryService service = new ProjectionQueryService(null);

        // When/Then
        assertThatThrownBy(() -> service.exists("officer", "BADGE-123"))
                .isInstanceOf(ProjectionQueryService.ProjectionQueryException.class)
                .hasMessageContaining("NATS query client is not available");
    }

    @Test
    void testGet_WithNullQueryClient_ThrowsException() {
        // Given
        ProjectionQueryService service = new ProjectionQueryService(null);

        // When/Then
        assertThatThrownBy(() -> service.get("officer", "BADGE-123"))
                .isInstanceOf(ProjectionQueryService.ProjectionQueryException.class)
                .hasMessageContaining("NATS query client is not available");
    }
}
