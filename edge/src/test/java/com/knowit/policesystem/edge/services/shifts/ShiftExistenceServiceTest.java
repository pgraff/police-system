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
 * Tests for ShiftExistenceService.
 */
@ExtendWith(MockitoExtension.class)
class ShiftExistenceServiceTest {

    @Mock
    private ProjectionQueryService projectionQueryService;

    private ShiftExistenceService shiftExistenceService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        shiftExistenceService = new ShiftExistenceService(projectionQueryService);
        
        // Set up log appender to capture log messages
        Logger logger = (Logger) LoggerFactory.getLogger(ShiftExistenceService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(Level.ERROR);
    }

    @Test
    void testExists_ShiftExists_ReturnsTrue() throws Exception {
        // Given
        String shiftId = "SHIFT-123";
        when(projectionQueryService.exists(eq("shift"), eq(shiftId))).thenReturn(true);

        // When
        boolean exists = shiftExistenceService.exists(shiftId);

        // Then
        assertThat(exists).isTrue();
        verify(projectionQueryService).exists("shift", shiftId);
    }

    @Test
    void testExists_ShiftNotExists_ReturnsFalse() throws Exception {
        // Given
        String shiftId = "SHIFT-456";
        when(projectionQueryService.exists(eq("shift"), eq(shiftId))).thenReturn(false);

        // When
        boolean exists = shiftExistenceService.exists(shiftId);

        // Then
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("shift", shiftId);
    }

    @Test
    void testExists_QueryFails_ReturnsFalse() throws Exception {
        // Given
        String shiftId = "SHIFT-789";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Connection failed");
        when(projectionQueryService.exists(eq("shift"), eq(shiftId))).thenThrow(exception);

        // When
        boolean exists = shiftExistenceService.exists(shiftId);

        // Then - should default to false on error
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("shift", shiftId);
    }

    @Test
    void testExists_LogsError_OnQueryFailure() throws Exception {
        // Given
        String shiftId = "SHIFT-999";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Database error");
        when(projectionQueryService.exists(eq("shift"), eq(shiftId))).thenThrow(exception);

        // When
        shiftExistenceService.exists(shiftId);

        // Then - verify error was logged
        assertThat(logAppender.list).isNotEmpty();
        assertThat(logAppender.list.get(0).getMessage()).contains("Failed to check shift existence");
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }
}
