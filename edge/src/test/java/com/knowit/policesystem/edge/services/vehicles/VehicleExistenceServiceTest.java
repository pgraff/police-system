package com.knowit.policesystem.edge.services.vehicles;

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
 * Tests for VehicleExistenceService.
 */
@ExtendWith(MockitoExtension.class)
class VehicleExistenceServiceTest {

    @Mock
    private ProjectionQueryService projectionQueryService;

    private VehicleExistenceService vehicleExistenceService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        vehicleExistenceService = new VehicleExistenceService(projectionQueryService);
        
        // Set up log appender to capture log messages
        Logger logger = (Logger) LoggerFactory.getLogger(VehicleExistenceService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(Level.ERROR);
    }

    @Test
    void testExists_VehicleExists_ReturnsTrue() throws Exception {
        // Given
        String unitId = "UNIT-123";
        when(projectionQueryService.exists(eq("vehicle"), eq(unitId))).thenReturn(true);

        // When
        boolean exists = vehicleExistenceService.exists(unitId);

        // Then
        assertThat(exists).isTrue();
        verify(projectionQueryService).exists("vehicle", unitId);
    }

    @Test
    void testExists_VehicleNotExists_ReturnsFalse() throws Exception {
        // Given
        String unitId = "UNIT-456";
        when(projectionQueryService.exists(eq("vehicle"), eq(unitId))).thenReturn(false);

        // When
        boolean exists = vehicleExistenceService.exists(unitId);

        // Then
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("vehicle", unitId);
    }

    @Test
    void testExists_QueryFails_ReturnsFalse() throws Exception {
        // Given
        String unitId = "UNIT-789";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Connection failed");
        when(projectionQueryService.exists(eq("vehicle"), eq(unitId))).thenThrow(exception);

        // When
        boolean exists = vehicleExistenceService.exists(unitId);

        // Then - should default to false on error
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("vehicle", unitId);
    }

    @Test
    void testExists_LogsError_OnQueryFailure() throws Exception {
        // Given
        String unitId = "UNIT-999";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Database error");
        when(projectionQueryService.exists(eq("vehicle"), eq(unitId))).thenThrow(exception);

        // When
        vehicleExistenceService.exists(unitId);

        // Then - verify error was logged
        assertThat(logAppender.list).isNotEmpty();
        assertThat(logAppender.list.get(0).getMessage()).contains("Failed to check vehicle existence");
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }
}
