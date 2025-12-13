package com.knowit.policesystem.edge.services.incidents;

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
 * Tests for IncidentExistenceService.
 */
@ExtendWith(MockitoExtension.class)
class IncidentExistenceServiceTest {

    @Mock
    private ProjectionQueryService projectionQueryService;

    private IncidentExistenceService incidentExistenceService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        incidentExistenceService = new IncidentExistenceService(projectionQueryService);
        
        // Set up log appender to capture log messages
        Logger logger = (Logger) LoggerFactory.getLogger(IncidentExistenceService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(Level.ERROR);
    }

    @Test
    void testExists_IncidentExists_ReturnsTrue() throws Exception {
        // Given
        String incidentId = "INC-123";
        when(projectionQueryService.exists(eq("incident"), eq(incidentId))).thenReturn(true);

        // When
        boolean exists = incidentExistenceService.exists(incidentId);

        // Then
        assertThat(exists).isTrue();
        verify(projectionQueryService).exists("incident", incidentId);
    }

    @Test
    void testExists_IncidentNotExists_ReturnsFalse() throws Exception {
        // Given
        String incidentId = "INC-456";
        when(projectionQueryService.exists(eq("incident"), eq(incidentId))).thenReturn(false);

        // When
        boolean exists = incidentExistenceService.exists(incidentId);

        // Then
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("incident", incidentId);
    }

    @Test
    void testExists_QueryFails_ReturnsFalse() throws Exception {
        // Given
        String incidentId = "INC-789";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Connection failed");
        when(projectionQueryService.exists(eq("incident"), eq(incidentId))).thenThrow(exception);

        // When
        boolean exists = incidentExistenceService.exists(incidentId);

        // Then - should default to false on error
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("incident", incidentId);
    }

    @Test
    void testExists_LogsError_OnQueryFailure() throws Exception {
        // Given
        String incidentId = "INC-999";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Database error");
        when(projectionQueryService.exists(eq("incident"), eq(incidentId))).thenThrow(exception);

        // When
        incidentExistenceService.exists(incidentId);

        // Then - verify error was logged
        assertThat(logAppender.list).isNotEmpty();
        assertThat(logAppender.list.get(0).getMessage()).contains("Failed to check incident existence");
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }
}
