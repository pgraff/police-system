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
import com.knowit.policesystem.projection.api.LocationProjectionPageResponse;
import com.knowit.policesystem.projection.api.LocationProjectionResponse;
import com.knowit.policesystem.projection.api.OfficerProjectionPageResponse;
import com.knowit.policesystem.projection.api.OfficerProjectionResponse;
import com.knowit.policesystem.projection.api.OfficerStatusHistoryResponse;
import com.knowit.policesystem.projection.api.PersonProjectionPageResponse;
import com.knowit.policesystem.projection.api.PersonProjectionResponse;
import com.knowit.policesystem.projection.api.UnitProjectionPageResponse;
import com.knowit.policesystem.projection.api.UnitProjectionResponse;
import com.knowit.policesystem.projection.api.UnitStatusHistoryResponse;
import com.knowit.policesystem.projection.api.VehicleProjectionPageResponse;
import com.knowit.policesystem.projection.api.VehicleProjectionResponse;
import com.knowit.policesystem.projection.api.VehicleStatusHistoryResponse;
import com.knowit.policesystem.projection.model.LocationProjectionEntity;
import com.knowit.policesystem.projection.model.OfficerProjectionEntity;
import com.knowit.policesystem.projection.model.OfficerStatusHistoryEntry;
import com.knowit.policesystem.projection.model.PersonProjectionEntity;
import com.knowit.policesystem.projection.model.UnitProjectionEntity;
import com.knowit.policesystem.projection.model.UnitStatusHistoryEntry;
import com.knowit.policesystem.projection.model.VehicleProjectionEntity;
import com.knowit.policesystem.projection.model.VehicleStatusHistoryEntry;
import com.knowit.policesystem.projection.repository.LocationProjectionRepository;
import com.knowit.policesystem.projection.repository.OfficerProjectionRepository;
import com.knowit.policesystem.projection.repository.PersonProjectionRepository;
import com.knowit.policesystem.projection.repository.UnitProjectionRepository;
import com.knowit.policesystem.projection.repository.VehicleProjectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for handling resource projection events.
 * Routes all resource events to appropriate repositories.
 */
@Service
public class ResourceProjectionService {

    private static final Logger log = LoggerFactory.getLogger(ResourceProjectionService.class);

    private final OfficerProjectionRepository officerRepository;
    private final VehicleProjectionRepository vehicleRepository;
    private final UnitProjectionRepository unitRepository;
    private final PersonProjectionRepository personRepository;
    private final LocationProjectionRepository locationRepository;
    private final Clock clock;

    public ResourceProjectionService(
            OfficerProjectionRepository officerRepository,
            VehicleProjectionRepository vehicleRepository,
            UnitProjectionRepository unitRepository,
            PersonProjectionRepository personRepository,
            LocationProjectionRepository locationRepository,
            Clock clock) {
        this.officerRepository = officerRepository;
        this.vehicleRepository = vehicleRepository;
        this.unitRepository = unitRepository;
        this.personRepository = personRepository;
        this.locationRepository = locationRepository;
        this.clock = clock;
    }

    public void handle(Object event) {
        if (event == null) {
            log.warn("Received null event");
            return;
        }

        // Officer events
        if (event instanceof RegisterOfficerRequested register) {
            handleRegisterOfficer(register);
            return;
        }
        if (event instanceof UpdateOfficerRequested update) {
            handleUpdateOfficer(update);
            return;
        }
        if (event instanceof ChangeOfficerStatusRequested changeStatus) {
            handleChangeOfficerStatus(changeStatus);
            return;
        }

        // Vehicle events
        if (event instanceof RegisterVehicleRequested register) {
            handleRegisterVehicle(register);
            return;
        }
        if (event instanceof UpdateVehicleRequested update) {
            handleUpdateVehicle(update);
            return;
        }
        if (event instanceof ChangeVehicleStatusRequested changeStatus) {
            handleChangeVehicleStatus(changeStatus);
            return;
        }

        // Unit events
        if (event instanceof CreateUnitRequested create) {
            handleCreateUnit(create);
            return;
        }
        if (event instanceof UpdateUnitRequested update) {
            handleUpdateUnit(update);
            return;
        }
        if (event instanceof ChangeUnitStatusRequested changeStatus) {
            handleChangeUnitStatus(changeStatus);
            return;
        }

        // Person events
        if (event instanceof RegisterPersonRequested register) {
            handleRegisterPerson(register);
            return;
        }
        if (event instanceof UpdatePersonRequested update) {
            handleUpdatePerson(update);
            return;
        }

        // Location events
        if (event instanceof CreateLocationRequested create) {
            handleCreateLocation(create);
            return;
        }
        if (event instanceof UpdateLocationRequested update) {
            handleUpdateLocation(update);
            return;
        }

        log.warn("Received unsupported event type: {}", event.getClass().getSimpleName());
    }

    // Officer event handlers
    private void handleRegisterOfficer(RegisterOfficerRequested event) {
        officerRepository.upsert(event, now());
    }

    private void handleUpdateOfficer(UpdateOfficerRequested event) {
        officerRepository.applyUpdate(event, now());
    }

    private void handleChangeOfficerStatus(ChangeOfficerStatusRequested event) {
        officerRepository.changeStatus(event, now());
    }

    // Vehicle event handlers
    private void handleRegisterVehicle(RegisterVehicleRequested event) {
        vehicleRepository.upsert(event, now());
    }

    private void handleUpdateVehicle(UpdateVehicleRequested event) {
        vehicleRepository.applyUpdate(event, now());
    }

    private void handleChangeVehicleStatus(ChangeVehicleStatusRequested event) {
        vehicleRepository.changeStatus(event, now());
    }

    // Unit event handlers
    private void handleCreateUnit(CreateUnitRequested event) {
        unitRepository.upsert(event, now());
    }

    private void handleUpdateUnit(UpdateUnitRequested event) {
        unitRepository.applyUpdate(event, now());
    }

    private void handleChangeUnitStatus(ChangeUnitStatusRequested event) {
        unitRepository.changeStatus(event, now());
    }

    // Person event handlers
    private void handleRegisterPerson(RegisterPersonRequested event) {
        personRepository.upsert(event, now());
    }

    private void handleUpdatePerson(UpdatePersonRequested event) {
        personRepository.applyUpdate(event, now());
    }

    // Location event handlers
    private void handleCreateLocation(CreateLocationRequested event) {
        locationRepository.upsert(event, now());
    }

    private void handleUpdateLocation(UpdateLocationRequested event) {
        locationRepository.applyUpdate(event, now());
    }

    // Officer query methods
    public Optional<OfficerProjectionResponse> getOfficer(String badgeNumber) {
        return officerRepository.findByBadgeNumber(badgeNumber)
                .map(this::toOfficerResponse);
    }

    public List<OfficerStatusHistoryResponse> getOfficerHistory(String badgeNumber) {
        return officerRepository.findHistory(badgeNumber).stream()
                .map(this::toOfficerStatusHistoryResponse)
                .collect(Collectors.toList());
    }

    public OfficerProjectionPageResponse listOfficers(String status, String rank, int page, int size) {
        List<OfficerProjectionResponse> content = officerRepository.findAll(status, rank, page, size).stream()
                .map(this::toOfficerResponse)
                .collect(Collectors.toList());
        long total = officerRepository.count(status, rank);
        return new OfficerProjectionPageResponse(content, total, page, size);
    }

    // Vehicle query methods
    public Optional<VehicleProjectionResponse> getVehicle(String unitId) {
        return vehicleRepository.findByUnitId(unitId)
                .map(this::toVehicleResponse);
    }

    public List<VehicleStatusHistoryResponse> getVehicleHistory(String unitId) {
        return vehicleRepository.findHistory(unitId).stream()
                .map(this::toVehicleStatusHistoryResponse)
                .collect(Collectors.toList());
    }

    public VehicleProjectionPageResponse listVehicles(String status, String vehicleType, int page, int size) {
        List<VehicleProjectionResponse> content = vehicleRepository.findAll(status, vehicleType, page, size).stream()
                .map(this::toVehicleResponse)
                .collect(Collectors.toList());
        long total = vehicleRepository.count(status, vehicleType);
        return new VehicleProjectionPageResponse(content, total, page, size);
    }

    // Unit query methods
    public Optional<UnitProjectionResponse> getUnit(String unitId) {
        return unitRepository.findByUnitId(unitId)
                .map(this::toUnitResponse);
    }

    public List<UnitStatusHistoryResponse> getUnitHistory(String unitId) {
        return unitRepository.findHistory(unitId).stream()
                .map(this::toUnitStatusHistoryResponse)
                .collect(Collectors.toList());
    }

    public UnitProjectionPageResponse listUnits(String status, String unitType, int page, int size) {
        List<UnitProjectionResponse> content = unitRepository.findAll(status, unitType, page, size).stream()
                .map(this::toUnitResponse)
                .collect(Collectors.toList());
        long total = unitRepository.count(status, unitType);
        return new UnitProjectionPageResponse(content, total, page, size);
    }

    // Person query methods
    public Optional<PersonProjectionResponse> getPerson(String personId) {
        return personRepository.findByPersonId(personId)
                .map(this::toPersonResponse);
    }

    public PersonProjectionPageResponse listPersons(String lastName, String firstName, int page, int size) {
        List<PersonProjectionResponse> content = personRepository.findAll(lastName, firstName, page, size).stream()
                .map(this::toPersonResponse)
                .collect(Collectors.toList());
        long total = personRepository.count(lastName, firstName);
        return new PersonProjectionPageResponse(content, total, page, size);
    }

    // Location query methods
    public Optional<LocationProjectionResponse> getLocation(String locationId) {
        return locationRepository.findByLocationId(locationId)
                .map(this::toLocationResponse);
    }

    public LocationProjectionPageResponse listLocations(String city, String state, int page, int size) {
        List<LocationProjectionResponse> content = locationRepository.findAll(city, state, page, size).stream()
                .map(this::toLocationResponse)
                .collect(Collectors.toList());
        long total = locationRepository.count(city, state);
        return new LocationProjectionPageResponse(content, total, page, size);
    }

    // Mapper methods
    private OfficerProjectionResponse toOfficerResponse(OfficerProjectionEntity entity) {
        return new OfficerProjectionResponse(
                entity.badgeNumber(),
                entity.firstName(),
                entity.lastName(),
                entity.rank(),
                entity.email(),
                entity.phoneNumber(),
                entity.hireDate(),
                entity.status(),
                entity.updatedAt()
        );
    }

    private OfficerStatusHistoryResponse toOfficerStatusHistoryResponse(OfficerStatusHistoryEntry entry) {
        return new OfficerStatusHistoryResponse(entry.status(), entry.changedAt());
    }

    private VehicleProjectionResponse toVehicleResponse(VehicleProjectionEntity entity) {
        return new VehicleProjectionResponse(
                entity.unitId(),
                entity.vehicleType(),
                entity.licensePlate(),
                entity.vin(),
                entity.status(),
                entity.lastMaintenanceDate(),
                entity.updatedAt()
        );
    }

    private VehicleStatusHistoryResponse toVehicleStatusHistoryResponse(VehicleStatusHistoryEntry entry) {
        return new VehicleStatusHistoryResponse(entry.status(), entry.changedAt());
    }

    private UnitProjectionResponse toUnitResponse(UnitProjectionEntity entity) {
        return new UnitProjectionResponse(
                entity.unitId(),
                entity.unitType(),
                entity.status(),
                entity.updatedAt()
        );
    }

    private UnitStatusHistoryResponse toUnitStatusHistoryResponse(UnitStatusHistoryEntry entry) {
        return new UnitStatusHistoryResponse(entry.status(), entry.changedAt());
    }

    private PersonProjectionResponse toPersonResponse(PersonProjectionEntity entity) {
        return new PersonProjectionResponse(
                entity.personId(),
                entity.firstName(),
                entity.lastName(),
                entity.dateOfBirth(),
                entity.gender(),
                entity.race(),
                entity.phoneNumber(),
                entity.updatedAt()
        );
    }

    private LocationProjectionResponse toLocationResponse(LocationProjectionEntity entity) {
        return new LocationProjectionResponse(
                entity.locationId(),
                entity.address(),
                entity.city(),
                entity.state(),
                entity.zipCode(),
                entity.latitude(),
                entity.longitude(),
                entity.locationType(),
                entity.updatedAt()
        );
    }

    private Instant now() {
        return clock.instant();
    }
}
