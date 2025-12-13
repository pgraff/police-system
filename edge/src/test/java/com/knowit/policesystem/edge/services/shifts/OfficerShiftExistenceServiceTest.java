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
 * Tests for OfficerShiftExistenceService.
 */
@ExtendWith(MockitoExtension.class)
class OfficerShiftExistenceServiceTest {

    @Mock
    private ProjectionQueryService projectionQueryService;

    private OfficerShiftExistenceService officerShiftExistenceService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        officerShiftExistenceService = new OfficerShiftExistenceService(projectionQueryService);
        
        // Set up log appender to capture log messages
        Logger logger = (Logger) LoggerFactory.getLogger(OfficerShiftExistenceService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(Level.ERROR);
    }

    @Test
    void testExists_OfficerShiftExists_ReturnsTrue() throws Exception {
        // Given
        Long id = 1L;
        when(projectionQueryService.exists(eq("officer-shift"), eq("1"))).thenReturn(true);

        // When
        boolean exists = officerShiftExistenceService.exists(id);

        // Then
        assertThat(exists).isTrue();
        verify(projectionQueryService).exists("officer-shift", "1");
    }

    @Test
    void testExists_OfficerShiftNotExists_ReturnsFalse() throws Exception {
        // Given
        Long id = 2L;
        when(projectionQueryService.exists(eq("officer-shift"), eq("2"))).thenReturn(false);

        // When
        boolean exists = officerShiftExistenceService.exists(id);

        // Then
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("officer-shift", "2");
    }

    @Test
    void testExists_QueryFails_ReturnsFalse() throws Exception {
        // Given
        Long id = 3L;
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Connection failed");
        when(projectionQueryService.exists(eq("officer-shift"), eq("3"))).thenThrow(exception);

        // When
        boolean exists = officerShiftExistenceService.exists(id);

        // Then - should default to false on error
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("officer-shift", "3");
    }

    @Test
    void testExists_LogsError_OnQueryFailure() throws Exception {
        // Given
        Long id = 4L;
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Database error");
        when(projectionQueryService.exists(eq("officer-shift"), eq("4"))).thenThrow(exception);

        // When
        officerShiftExistenceService.exists(id);

        // Then - verify error was logged
        assertThat(logAppender.list).isNotEmpty();
        assertThat(logAppender.list.get(0).getMessage()).contains("Failed to check officer shift existence");
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }
}
