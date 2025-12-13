package com.knowit.policesystem.edge.services.dispatches;

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
 * Tests for DispatchExistenceService.
 */
@ExtendWith(MockitoExtension.class)
class DispatchExistenceServiceTest {

    @Mock
    private ProjectionQueryService projectionQueryService;

    private DispatchExistenceService dispatchExistenceService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        dispatchExistenceService = new DispatchExistenceService(projectionQueryService);
        
        // Set up log appender to capture log messages
        Logger logger = (Logger) LoggerFactory.getLogger(DispatchExistenceService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(Level.ERROR);
    }

    @Test
    void testExists_DispatchExists_ReturnsTrue() throws Exception {
        // Given
        String dispatchId = "DISP-123";
        when(projectionQueryService.exists(eq("dispatch"), eq(dispatchId))).thenReturn(true);

        // When
        boolean exists = dispatchExistenceService.exists(dispatchId);

        // Then
        assertThat(exists).isTrue();
        verify(projectionQueryService).exists("dispatch", dispatchId);
    }

    @Test
    void testExists_DispatchNotExists_ReturnsFalse() throws Exception {
        // Given
        String dispatchId = "DISP-456";
        when(projectionQueryService.exists(eq("dispatch"), eq(dispatchId))).thenReturn(false);

        // When
        boolean exists = dispatchExistenceService.exists(dispatchId);

        // Then
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("dispatch", dispatchId);
    }

    @Test
    void testExists_QueryFails_ReturnsFalse() throws Exception {
        // Given
        String dispatchId = "DISP-789";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Connection failed");
        when(projectionQueryService.exists(eq("dispatch"), eq(dispatchId))).thenThrow(exception);

        // When
        boolean exists = dispatchExistenceService.exists(dispatchId);

        // Then - should default to false on error
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("dispatch", dispatchId);
    }

    @Test
    void testExists_LogsError_OnQueryFailure() throws Exception {
        // Given
        String dispatchId = "DISP-999";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Database error");
        when(projectionQueryService.exists(eq("dispatch"), eq(dispatchId))).thenThrow(exception);

        // When
        dispatchExistenceService.exists(dispatchId);

        // Then - verify error was logged
        assertThat(logAppender.list).isNotEmpty();
        assertThat(logAppender.list.get(0).getMessage()).contains("Failed to check dispatch existence");
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }
}
