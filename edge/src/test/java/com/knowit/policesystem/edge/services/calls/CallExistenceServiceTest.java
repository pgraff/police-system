package com.knowit.policesystem.edge.services.calls;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for ProjectionCallExistenceService.
 */
@ExtendWith(MockitoExtension.class)
class CallExistenceServiceTest {

    @Mock
    private ProjectionQueryService projectionQueryService;

    private ProjectionCallExistenceService callExistenceService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        callExistenceService = new ProjectionCallExistenceService(projectionQueryService);
        
        // Set up log appender to capture log messages
        Logger logger = (Logger) LoggerFactory.getLogger(ProjectionCallExistenceService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(Level.ERROR);
    }

    @Test
    void testExists_CallExists_ReturnsTrue() throws Exception {
        // Given
        String callId = "CALL-123";
        when(projectionQueryService.exists(eq("call"), eq(callId))).thenReturn(true);

        // When
        boolean exists = callExistenceService.exists(callId);

        // Then
        assertThat(exists).isTrue();
        verify(projectionQueryService).exists("call", callId);
    }

    @Test
    void testExists_CallNotExists_ReturnsFalse() throws Exception {
        // Given
        String callId = "CALL-456";
        when(projectionQueryService.exists(eq("call"), eq(callId))).thenReturn(false);

        // When
        boolean exists = callExistenceService.exists(callId);

        // Then
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("call", callId);
    }

    @Test
    void testExists_QueryFails_ReturnsFalse() throws Exception {
        // Given
        String callId = "CALL-789";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Connection failed");
        when(projectionQueryService.exists(eq("call"), eq(callId))).thenThrow(exception);

        // When
        boolean exists = callExistenceService.exists(callId);

        // Then - should default to false on error
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("call", callId);
    }

    @Test
    void testExists_LogsError_OnQueryFailure() throws Exception {
        // Given
        String callId = "CALL-999";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Database error");
        when(projectionQueryService.exists(eq("call"), eq(callId))).thenThrow(exception);

        // When
        callExistenceService.exists(callId);

        // Then - verify error was logged
        assertThat(logAppender.list).isNotEmpty();
        assertThat(logAppender.list.get(0).getMessage()).contains("Failed to check call existence");
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }
}
