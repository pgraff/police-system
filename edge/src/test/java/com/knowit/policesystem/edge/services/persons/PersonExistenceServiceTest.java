package com.knowit.policesystem.edge.services.persons;

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
 * Tests for PersonExistenceService.
 */
@ExtendWith(MockitoExtension.class)
class PersonExistenceServiceTest {

    @Mock
    private ProjectionQueryService projectionQueryService;

    private PersonExistenceService personExistenceService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        personExistenceService = new PersonExistenceService(projectionQueryService);
        
        // Set up log appender to capture log messages
        Logger logger = (Logger) LoggerFactory.getLogger(PersonExistenceService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(Level.ERROR);
    }

    @Test
    void testExists_PersonExists_ReturnsTrue() throws Exception {
        // Given
        String personId = "PERSON-123";
        when(projectionQueryService.exists(eq("person"), eq(personId))).thenReturn(true);

        // When
        boolean exists = personExistenceService.exists(personId);

        // Then
        assertThat(exists).isTrue();
        verify(projectionQueryService).exists("person", personId);
    }

    @Test
    void testExists_PersonNotExists_ReturnsFalse() throws Exception {
        // Given
        String personId = "PERSON-456";
        when(projectionQueryService.exists(eq("person"), eq(personId))).thenReturn(false);

        // When
        boolean exists = personExistenceService.exists(personId);

        // Then
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("person", personId);
    }

    @Test
    void testExists_QueryFails_ReturnsFalse() throws Exception {
        // Given
        String personId = "PERSON-789";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Connection failed");
        when(projectionQueryService.exists(eq("person"), eq(personId))).thenThrow(exception);

        // When
        boolean exists = personExistenceService.exists(personId);

        // Then - should default to false on error
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("person", personId);
    }

    @Test
    void testExists_LogsError_OnQueryFailure() throws Exception {
        // Given
        String personId = "PERSON-999";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Database error");
        when(projectionQueryService.exists(eq("person"), eq(personId))).thenThrow(exception);

        // When
        personExistenceService.exists(personId);

        // Then - verify error was logged
        assertThat(logAppender.list).isNotEmpty();
        assertThat(logAppender.list.get(0).getMessage()).contains("Failed to check person existence");
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }
}
