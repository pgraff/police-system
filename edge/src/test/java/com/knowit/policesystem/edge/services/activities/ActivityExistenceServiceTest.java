package com.knowit.policesystem.edge.services.activities;

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
 * Tests for ActivityExistenceService.
 */
@ExtendWith(MockitoExtension.class)
class ActivityExistenceServiceTest {

    @Mock
    private ProjectionQueryService projectionQueryService;

    private ActivityExistenceService activityExistenceService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        activityExistenceService = new ActivityExistenceService(projectionQueryService);
        
        // Set up log appender to capture log messages
        Logger logger = (Logger) LoggerFactory.getLogger(ActivityExistenceService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(Level.ERROR);
    }

    @Test
    void testExists_ActivityExists_ReturnsTrue() throws Exception {
        // Given
        String activityId = "ACT-123";
        when(projectionQueryService.exists(eq("activity"), eq(activityId))).thenReturn(true);

        // When
        boolean exists = activityExistenceService.exists(activityId);

        // Then
        assertThat(exists).isTrue();
        verify(projectionQueryService).exists("activity", activityId);
    }

    @Test
    void testExists_ActivityNotExists_ReturnsFalse() throws Exception {
        // Given
        String activityId = "ACT-456";
        when(projectionQueryService.exists(eq("activity"), eq(activityId))).thenReturn(false);

        // When
        boolean exists = activityExistenceService.exists(activityId);

        // Then
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("activity", activityId);
    }

    @Test
    void testExists_QueryFails_ReturnsFalse() throws Exception {
        // Given
        String activityId = "ACT-789";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Connection failed");
        when(projectionQueryService.exists(eq("activity"), eq(activityId))).thenThrow(exception);

        // When
        boolean exists = activityExistenceService.exists(activityId);

        // Then - should default to false on error
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("activity", activityId);
    }

    @Test
    void testExists_LogsError_OnQueryFailure() throws Exception {
        // Given
        String activityId = "ACT-999";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Database error");
        when(projectionQueryService.exists(eq("activity"), eq(activityId))).thenThrow(exception);

        // When
        activityExistenceService.exists(activityId);

        // Then - verify error was logged
        assertThat(logAppender.list).isNotEmpty();
        assertThat(logAppender.list.get(0).getMessage()).contains("Failed to check activity existence");
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }
}
