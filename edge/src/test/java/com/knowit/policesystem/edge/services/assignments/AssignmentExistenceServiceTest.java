package com.knowit.policesystem.edge.services.assignments;

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
 * Tests for AssignmentExistenceService.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentExistenceServiceTest {

    @Mock
    private ProjectionQueryService projectionQueryService;

    private AssignmentExistenceService assignmentExistenceService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        assignmentExistenceService = new AssignmentExistenceService(projectionQueryService);
        
        // Set up log appender to capture log messages
        Logger logger = (Logger) LoggerFactory.getLogger(AssignmentExistenceService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(Level.ERROR);
    }

    @Test
    void testExists_AssignmentExists_ReturnsTrue() throws Exception {
        // Given
        String assignmentId = "ASSIGN-123";
        when(projectionQueryService.exists(eq("assignment"), eq(assignmentId))).thenReturn(true);

        // When
        boolean exists = assignmentExistenceService.exists(assignmentId);

        // Then
        assertThat(exists).isTrue();
        verify(projectionQueryService).exists("assignment", assignmentId);
    }

    @Test
    void testExists_AssignmentNotExists_ReturnsFalse() throws Exception {
        // Given
        String assignmentId = "ASSIGN-456";
        when(projectionQueryService.exists(eq("assignment"), eq(assignmentId))).thenReturn(false);

        // When
        boolean exists = assignmentExistenceService.exists(assignmentId);

        // Then
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("assignment", assignmentId);
    }

    @Test
    void testExists_QueryFails_ReturnsFalse() throws Exception {
        // Given
        String assignmentId = "ASSIGN-789";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Connection failed");
        when(projectionQueryService.exists(eq("assignment"), eq(assignmentId))).thenThrow(exception);

        // When
        boolean exists = assignmentExistenceService.exists(assignmentId);

        // Then - should default to false on error
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("assignment", assignmentId);
    }

    @Test
    void testExists_LogsError_OnQueryFailure() throws Exception {
        // Given
        String assignmentId = "ASSIGN-999";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Database error");
        when(projectionQueryService.exists(eq("assignment"), eq(assignmentId))).thenThrow(exception);

        // When
        assignmentExistenceService.exists(assignmentId);

        // Then - verify error was logged
        assertThat(logAppender.list).isNotEmpty();
        assertThat(logAppender.list.get(0).getMessage()).contains("Failed to check assignment existence");
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }
}
