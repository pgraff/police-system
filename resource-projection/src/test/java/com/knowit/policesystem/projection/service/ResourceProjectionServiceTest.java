package com.knowit.policesystem.projection.service;

import com.knowit.policesystem.common.events.locations.CreateLocationRequested;
import com.knowit.policesystem.common.events.locations.UpdateLocationRequested;
import com.knowit.policesystem.common.events.officers.ChangeOfficerStatusRequested;
import com.knowit.policesystem.common.events.officers.RegisterOfficerRequested;
import com.knowit.policesystem.common.events.officers.UpdateOfficerRequested;
import com.knowit.policesystem.common.events.persons.RegisterPersonRequested;
import com.knowit.policesystem.common.events.persons.UpdatePersonRequested;
import com.knowit.policesystem.common.events.units.ChangeUnitStatusRequested;
import com.knowit.policesystem.common.events.units.CreateUnitRequested;
import com.knowit.policesystem.common.events.units.UpdateUnitRequested;
import com.knowit.policesystem.common.events.vehicles.ChangeVehicleStatusRequested;
import com.knowit.policesystem.common.events.vehicles.RegisterVehicleRequested;
import com.knowit.policesystem.common.events.vehicles.UpdateVehicleRequested;
import com.knowit.policesystem.projection.model.OfficerProjectionEntity;
import com.knowit.policesystem.projection.model.OfficerStatusHistoryEntry;
import com.knowit.policesystem.projection.model.VehicleProjectionEntity;
import com.knowit.policesystem.projection.model.VehicleStatusHistoryEntry;
import com.knowit.policesystem.projection.model.UnitProjectionEntity;
import com.knowit.policesystem.projection.model.UnitStatusHistoryEntry;
import com.knowit.policesystem.projection.model.PersonProjectionEntity;
import com.knowit.policesystem.projection.model.LocationProjectionEntity;
import com.knowit.policesystem.projection.repository.LocationProjectionRepository;
import com.knowit.policesystem.projection.repository.OfficerProjectionRepository;
import com.knowit.policesystem.projection.repository.PersonProjectionRepository;
import com.knowit.policesystem.projection.repository.UnitProjectionRepository;
import com.knowit.policesystem.projection.repository.VehicleProjectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceProjectionServiceTest {

    @Mock
    private OfficerProjectionRepository officerRepository;

    @Mock
    private VehicleProjectionRepository vehicleRepository;

    @Mock
    private UnitProjectionRepository unitRepository;

    @Mock
    private PersonProjectionRepository personRepository;

    @Mock
    private LocationProjectionRepository locationRepository;

    private Clock clock;
    private ResourceProjectionService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2024-01-01T12:00:00Z"), ZoneId.of("UTC"));
        service = new ResourceProjectionService(
                officerRepository,
                vehicleRepository,
                unitRepository,
                personRepository,
                locationRepository,
                clock
        );
    }

    @Test
    void handle_WithRegisterOfficerRequested_CallsRepositoryUpsert() {
        RegisterOfficerRequested event = new RegisterOfficerRequested(
                "BADGE-001", "John", "Doe", "Sergeant", "john.doe@police.gov",
                "555-0100", "2020-01-15", "Active"
        );

        service.handle(event);

        ArgumentCaptor<Instant> timestampCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(officerRepository).upsert(eq(event), timestampCaptor.capture());
        assertThat(timestampCaptor.getValue()).isEqualTo(clock.instant());
    }

    @Test
    void handle_WithUpdateOfficerRequested_CallsRepositoryApplyUpdate() {
        UpdateOfficerRequested event = new UpdateOfficerRequested(
                "BADGE-001", null, "Smith", "Lieutenant", null, null, null
        );

        service.handle(event);

        verify(officerRepository).applyUpdate(eq(event), any(Instant.class));
    }

    @Test
    void handle_WithChangeOfficerStatusRequested_CallsRepositoryChangeStatus() {
        ChangeOfficerStatusRequested event = new ChangeOfficerStatusRequested("BADGE-001", "On-Duty");

        service.handle(event);

        verify(officerRepository).changeStatus(eq(event), any(Instant.class));
    }

    @Test
    void handle_WithRegisterVehicleRequested_CallsRepositoryUpsert() {
        RegisterVehicleRequested event = new RegisterVehicleRequested(
                "UNIT-001", "Patrol", "ABC-123", "1HGBH41JXMN109186", "Available", "2024-01-15"
        );

        service.handle(event);

        verify(vehicleRepository).upsert(eq(event), any(Instant.class));
    }

    @Test
    void handle_WithCreateUnitRequested_CallsRepositoryUpsert() {
        CreateUnitRequested event = new CreateUnitRequested("UNIT-001", "Patrol", "Available");

        service.handle(event);

        verify(unitRepository).upsert(eq(event), any(Instant.class));
    }

    @Test
    void handle_WithRegisterPersonRequested_CallsRepositoryUpsert() {
        RegisterPersonRequested event = new RegisterPersonRequested(
                "PERSON-001", "PERSON-001", "John", "Doe", "1990-01-15", "Male", "White", "555-0100"
        );

        service.handle(event);

        verify(personRepository).upsert(eq(event), any(Instant.class));
    }

    @Test
    void handle_WithCreateLocationRequested_CallsRepositoryUpsert() {
        CreateLocationRequested event = new CreateLocationRequested(
                "LOC-001", "LOC-001", "123 Main St", "Springfield", "IL", "62701",
                "39.7817", "-89.6501", "Address"
        );

        service.handle(event);

        verify(locationRepository).upsert(eq(event), any(Instant.class));
    }

    @Test
    void getOfficer_WithValidBadgeNumber_ReturnsResponse() {
        String badgeNumber = "BADGE-001";
        OfficerProjectionEntity entity = new OfficerProjectionEntity(
                badgeNumber, "John", "Doe", "Sergeant", "john.doe@police.gov",
                "555-0100", "2020-01-15", "Active", Instant.now()
        );
        when(officerRepository.findByBadgeNumber(badgeNumber)).thenReturn(Optional.of(entity));

        var result = service.getOfficer(badgeNumber);

        assertThat(result).isPresent();
        assertThat(result.get().badgeNumber()).isEqualTo(badgeNumber);
        assertThat(result.get().firstName()).isEqualTo("John");
    }

    @Test
    void getOfficerHistory_ReturnsHistory() {
        String badgeNumber = "BADGE-001";
        List<OfficerStatusHistoryEntry> history = List.of(
                new OfficerStatusHistoryEntry("Active", Instant.now()),
                new OfficerStatusHistoryEntry("On-Duty", Instant.now())
        );
        when(officerRepository.findHistory(badgeNumber)).thenReturn(history);

        var result = service.getOfficerHistory(badgeNumber);

        assertThat(result).hasSize(2);
    }

    @Test
    void listOfficers_ReturnsPageResponse() {
        OfficerProjectionEntity entity = new OfficerProjectionEntity(
                "BADGE-001", "John", "Doe", "Sergeant", "john.doe@police.gov",
                "555-0100", "2020-01-15", "Active", Instant.now()
        );
        when(officerRepository.findAll(null, null, 0, 20)).thenReturn(List.of(entity));
        when(officerRepository.count(null, null)).thenReturn(1L);

        var result = service.listOfficers(null, null, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.total()).isEqualTo(1L);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
    }

    @Test
    void getVehicle_WithValidUnitId_ReturnsResponse() {
        String unitId = "UNIT-001";
        VehicleProjectionEntity entity = new VehicleProjectionEntity(
                unitId, "Patrol", "ABC-123", "1HGBH41JXMN109186", "Available", "2024-01-15", Instant.now()
        );
        when(vehicleRepository.findByUnitId(unitId)).thenReturn(Optional.of(entity));

        var result = service.getVehicle(unitId);

        assertThat(result).isPresent();
        assertThat(result.get().unitId()).isEqualTo(unitId);
    }

    @Test
    void getPerson_WithValidPersonId_ReturnsResponse() {
        String personId = "PERSON-001";
        PersonProjectionEntity entity = new PersonProjectionEntity(
                personId, "John", "Doe", "1990-01-15", "Male", "White", "555-0100", Instant.now()
        );
        when(personRepository.findByPersonId(personId)).thenReturn(Optional.of(entity));

        var result = service.getPerson(personId);

        assertThat(result).isPresent();
        assertThat(result.get().personId()).isEqualTo(personId);
    }

    @Test
    void getLocation_WithValidLocationId_ReturnsResponse() {
        String locationId = "LOC-001";
        LocationProjectionEntity entity = new LocationProjectionEntity(
                locationId, "123 Main St", "Springfield", "IL", "62701",
                "39.7817", "-89.6501", "Address", Instant.now()
        );
        when(locationRepository.findByLocationId(locationId)).thenReturn(Optional.of(entity));

        var result = service.getLocation(locationId);

        assertThat(result).isPresent();
        assertThat(result.get().locationId()).isEqualTo(locationId);
    }

    @Test
    void handle_WithUnsupportedEvent_LogsWarning() {
        Object unsupportedEvent = new Object();

        service.handle(unsupportedEvent);

        // Should not throw exception, just log warning
    }
}
