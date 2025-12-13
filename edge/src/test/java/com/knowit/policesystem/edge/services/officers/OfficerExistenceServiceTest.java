package com.knowit.policesystem.edge.services.officers;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for OfficerExistenceService.
 */
@ExtendWith(MockitoExtension.class)
class OfficerExistenceServiceTest {

    @Mock
    private ProjectionQueryService projectionQueryService;

    private OfficerExistenceService officerExistenceService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        officerExistenceService = new OfficerExistenceService(projectionQueryService);
        
        // Set up log appender to capture log messages
        Logger logger = (Logger) LoggerFactory.getLogger(OfficerExistenceService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(Level.ERROR);
    }

    @Test
    void testExists_OfficerExists_ReturnsTrue() throws Exception {
        // Given
        String badgeNumber = "BADGE-123";
        when(projectionQueryService.exists(eq("officer"), eq(badgeNumber))).thenReturn(true);

        // When
        boolean exists = officerExistenceService.exists(badgeNumber);

        // Then
        assertThat(exists).isTrue();
        verify(projectionQueryService).exists("officer", badgeNumber);
    }

    @Test
    void testExists_OfficerNotExists_ReturnsFalse() throws Exception {
        // Given
        String badgeNumber = "BADGE-456";
        when(projectionQueryService.exists(eq("officer"), eq(badgeNumber))).thenReturn(false);

        // When
        boolean exists = officerExistenceService.exists(badgeNumber);

        // Then
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("officer", badgeNumber);
    }

    @Test
    void testExists_QueryFails_ReturnsFalse() throws Exception {
        // Given
        String badgeNumber = "BADGE-789";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Connection failed");
        when(projectionQueryService.exists(eq("officer"), eq(badgeNumber))).thenThrow(exception);

        // When
        boolean exists = officerExistenceService.exists(badgeNumber);

        // Then - should default to false on error
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("officer", badgeNumber);
    }

    @Test
    void testExists_LogsError_OnQueryFailure() throws Exception {
        // Given
        String badgeNumber = "BADGE-999";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Database error");
        when(projectionQueryService.exists(eq("officer"), eq(badgeNumber))).thenThrow(exception);

        // When
        officerExistenceService.exists(badgeNumber);

        // Then - verify error was logged
        assertThat(logAppender.list).isNotEmpty();
        assertThat(logAppender.list.get(0).getMessage()).contains("Failed to check officer existence");
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }
}
