package com.knowit.policesystem.edge.services.shifts;

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
 * Tests for ShiftChangeExistenceService.
 */
@ExtendWith(MockitoExtension.class)
class ShiftChangeExistenceServiceTest {

    @Mock
    private ProjectionQueryService projectionQueryService;

    private ShiftChangeExistenceService shiftChangeExistenceService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        shiftChangeExistenceService = new ShiftChangeExistenceService(projectionQueryService);
        
        // Set up log appender to capture log messages
        Logger logger = (Logger) LoggerFactory.getLogger(ShiftChangeExistenceService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(Level.ERROR);
    }

    @Test
    void testExists_ShiftChangeExists_ReturnsTrue() throws Exception {
        // Given
        String shiftChangeId = "CHANGE-123";
        when(projectionQueryService.exists(eq("shift-change"), eq(shiftChangeId))).thenReturn(true);

        // When
        boolean exists = shiftChangeExistenceService.exists(shiftChangeId);

        // Then
        assertThat(exists).isTrue();
        verify(projectionQueryService).exists("shift-change", shiftChangeId);
    }

    @Test
    void testExists_ShiftChangeNotExists_ReturnsFalse() throws Exception {
        // Given
        String shiftChangeId = "CHANGE-456";
        when(projectionQueryService.exists(eq("shift-change"), eq(shiftChangeId))).thenReturn(false);

        // When
        boolean exists = shiftChangeExistenceService.exists(shiftChangeId);

        // Then
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("shift-change", shiftChangeId);
    }

    @Test
    void testExists_QueryFails_ReturnsFalse() throws Exception {
        // Given
        String shiftChangeId = "CHANGE-789";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Connection failed");
        when(projectionQueryService.exists(eq("shift-change"), eq(shiftChangeId))).thenThrow(exception);

        // When
        boolean exists = shiftChangeExistenceService.exists(shiftChangeId);

        // Then - should default to false on error
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("shift-change", shiftChangeId);
    }

    @Test
    void testExists_LogsError_OnQueryFailure() throws Exception {
        // Given
        String shiftChangeId = "CHANGE-999";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Database error");
        when(projectionQueryService.exists(eq("shift-change"), eq(shiftChangeId))).thenThrow(exception);

        // When
        shiftChangeExistenceService.exists(shiftChangeId);

        // Then - verify error was logged
        assertThat(logAppender.list).isNotEmpty();
        assertThat(logAppender.list.get(0).getMessage()).contains("Failed to check shift change existence");
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }
}
