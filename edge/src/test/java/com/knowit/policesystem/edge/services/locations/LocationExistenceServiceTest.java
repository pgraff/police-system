package com.knowit.policesystem.edge.services.locations;

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
 * Tests for LocationExistenceService.
 */
@ExtendWith(MockitoExtension.class)
class LocationExistenceServiceTest {

    @Mock
    private ProjectionQueryService projectionQueryService;

    private LocationExistenceService locationExistenceService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        locationExistenceService = new LocationExistenceService(projectionQueryService);
        
        // Set up log appender to capture log messages
        Logger logger = (Logger) LoggerFactory.getLogger(LocationExistenceService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(Level.ERROR);
    }

    @Test
    void testExists_LocationExists_ReturnsTrue() throws Exception {
        // Given
        String locationId = "LOC-123";
        when(projectionQueryService.exists(eq("location"), eq(locationId))).thenReturn(true);

        // When
        boolean exists = locationExistenceService.exists(locationId);

        // Then
        assertThat(exists).isTrue();
        verify(projectionQueryService).exists("location", locationId);
    }

    @Test
    void testExists_LocationNotExists_ReturnsFalse() throws Exception {
        // Given
        String locationId = "LOC-456";
        when(projectionQueryService.exists(eq("location"), eq(locationId))).thenReturn(false);

        // When
        boolean exists = locationExistenceService.exists(locationId);

        // Then
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("location", locationId);
    }

    @Test
    void testExists_QueryFails_ReturnsFalse() throws Exception {
        // Given
        String locationId = "LOC-789";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Connection failed");
        when(projectionQueryService.exists(eq("location"), eq(locationId))).thenThrow(exception);

        // When
        boolean exists = locationExistenceService.exists(locationId);

        // Then - should default to false on error
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("location", locationId);
    }

    @Test
    void testExists_LogsError_OnQueryFailure() throws Exception {
        // Given
        String locationId = "LOC-999";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Database error");
        when(projectionQueryService.exists(eq("location"), eq(locationId))).thenThrow(exception);

        // When
        locationExistenceService.exists(locationId);

        // Then - verify error was logged
        assertThat(logAppender.list).isNotEmpty();
        assertThat(logAppender.list.get(0).getMessage()).contains("Failed to check location existence");
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }
}
