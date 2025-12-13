package com.knowit.policesystem.projection.api;

import com.knowit.policesystem.projection.service.ResourceProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceProjectionControllerTest {

    @Mock
    private ResourceProjectionService projectionService;

    @InjectMocks
    private ResourceProjectionController controller;

    @Test
    void getOfficer_WithValidBadgeNumber_ReturnsOk() {
        OfficerProjectionResponse response = new OfficerProjectionResponse(
                "BADGE-001", "John", "Doe", "Sergeant", "john.doe@police.gov",
                "555-0100", "2020-01-15", "Active", Instant.now()
        );
        when(projectionService.getOfficer("BADGE-001")).thenReturn(Optional.of(response));

        ResponseEntity<OfficerProjectionResponse> result = controller.getOfficer("BADGE-001");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().badgeNumber()).isEqualTo("BADGE-001");
    }

    @Test
    void getOfficer_WithInvalidBadgeNumber_ReturnsNotFound() {
        when(projectionService.getOfficer("INVALID")).thenReturn(Optional.empty());

        ResponseEntity<OfficerProjectionResponse> result = controller.getOfficer("INVALID");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getOfficerHistory_WithValidBadgeNumber_ReturnsOk() {
        OfficerProjectionResponse projection = new OfficerProjectionResponse(
                "BADGE-001", "John", "Doe", "Sergeant", "john.doe@police.gov",
                "555-0100", "2020-01-15", "Active", Instant.now()
        );
        List<OfficerStatusHistoryResponse> history = List.of(
                new OfficerStatusHistoryResponse("Active", Instant.now()),
                new OfficerStatusHistoryResponse("On-Duty", Instant.now())
        );
        when(projectionService.getOfficer("BADGE-001")).thenReturn(Optional.of(projection));
        when(projectionService.getOfficerHistory("BADGE-001")).thenReturn(history);

        ResponseEntity<List<OfficerStatusHistoryResponse>> result = controller.getOfficerHistory("BADGE-001");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(2);
    }

    @Test
    void listOfficers_WithFilters_ReturnsOk() {
        OfficerProjectionPageResponse pageResponse = new OfficerProjectionPageResponse(
                List.of(new OfficerProjectionResponse(
                        "BADGE-001", "John", "Doe", "Sergeant", "john.doe@police.gov",
                        "555-0100", "2020-01-15", "Active", Instant.now()
                )),
                1L, 0, 20
        );
        when(projectionService.listOfficers("Active", null, 0, 20)).thenReturn(pageResponse);

        ResponseEntity<OfficerProjectionPageResponse> result = controller.listOfficers("Active", null, 0, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().content()).hasSize(1);
    }

    @Test
    void listOfficers_WithInvalidPagination_BoundsParameters() {
        OfficerProjectionPageResponse pageResponse = new OfficerProjectionPageResponse(
                List.of(), 0L, 0, 1
        );
        when(projectionService.listOfficers(isNull(), isNull(), anyInt(), anyInt())).thenReturn(pageResponse);

        ResponseEntity<OfficerProjectionPageResponse> result = controller.listOfficers(null, null, -1, 300);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(projectionService).listOfficers(isNull(), isNull(), anyInt(), anyInt());
    }

    @Test
    void getVehicle_WithValidUnitId_ReturnsOk() {
        VehicleProjectionResponse response = new VehicleProjectionResponse(
                "UNIT-001", "Patrol", "ABC-123", "1HGBH41JXMN109186", "Available", "2024-01-15", Instant.now()
        );
        when(projectionService.getVehicle("UNIT-001")).thenReturn(Optional.of(response));

        ResponseEntity<VehicleProjectionResponse> result = controller.getVehicle("UNIT-001");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().unitId()).isEqualTo("UNIT-001");
    }

    @Test
    void getUnit_WithValidUnitId_ReturnsOk() {
        UnitProjectionResponse response = new UnitProjectionResponse(
                "UNIT-001", "Patrol", "Available", Instant.now()
        );
        when(projectionService.getUnit("UNIT-001")).thenReturn(Optional.of(response));

        ResponseEntity<UnitProjectionResponse> result = controller.getUnit("UNIT-001");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().unitId()).isEqualTo("UNIT-001");
    }

    @Test
    void getPerson_WithValidPersonId_ReturnsOk() {
        PersonProjectionResponse response = new PersonProjectionResponse(
                "PERSON-001", "John", "Doe", "1990-01-15", "Male", "White", "555-0100", Instant.now()
        );
        when(projectionService.getPerson("PERSON-001")).thenReturn(Optional.of(response));

        ResponseEntity<PersonProjectionResponse> result = controller.getPerson("PERSON-001");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().personId()).isEqualTo("PERSON-001");
    }

    @Test
    void getLocation_WithValidLocationId_ReturnsOk() {
        LocationProjectionResponse response = new LocationProjectionResponse(
                "LOC-001", "123 Main St", "Springfield", "IL", "62701",
                "39.7817", "-89.6501", "Address", Instant.now()
        );
        when(projectionService.getLocation("LOC-001")).thenReturn(Optional.of(response));

        ResponseEntity<LocationProjectionResponse> result = controller.getLocation("LOC-001");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().locationId()).isEqualTo("LOC-001");
    }
}
