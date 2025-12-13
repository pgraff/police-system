package com.knowit.policesystem.edge.services.officers;

import com.knowit.policesystem.edge.services.projections.ProjectionQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for OfficerConflictService.
 */
@ExtendWith(MockitoExtension.class)
class OfficerConflictServiceTest {

    @Mock
    private ProjectionQueryService projectionQueryService;

    private OfficerConflictService officerConflictService;

    @BeforeEach
    void setUp() {
        officerConflictService = new OfficerConflictService(projectionQueryService);
    }

    @Test
    void testBadgeNumberExists_Exists_ReturnsTrue() throws Exception {
        // Given
        String badgeNumber = "BADGE-123";
        when(projectionQueryService.exists(eq("officer"), eq(badgeNumber))).thenReturn(true);

        // When
        boolean exists = officerConflictService.badgeNumberExists(badgeNumber);

        // Then
        assertThat(exists).isTrue();
        verify(projectionQueryService).exists("officer", badgeNumber);
    }

    @Test
    void testBadgeNumberExists_NotExists_ReturnsFalse() throws Exception {
        // Given
        String badgeNumber = "BADGE-456";
        when(projectionQueryService.exists(eq("officer"), eq(badgeNumber))).thenReturn(false);

        // When
        boolean exists = officerConflictService.badgeNumberExists(badgeNumber);

        // Then
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("officer", badgeNumber);
    }

    @Test
    void testBadgeNumberExists_QueryFails_ReturnsFalse() throws Exception {
        // Given
        String badgeNumber = "BADGE-789";
        ProjectionQueryService.ProjectionQueryException exception = 
                new ProjectionQueryService.ProjectionQueryException("Connection failed");
        when(projectionQueryService.exists(eq("officer"), eq(badgeNumber))).thenThrow(exception);

        // When
        boolean exists = officerConflictService.badgeNumberExists(badgeNumber);

        // Then - should default to false on error (allows creation to proceed)
        assertThat(exists).isFalse();
        verify(projectionQueryService).exists("officer", badgeNumber);
    }
}
