package com.knowit.policesystem.projection.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceEventParserTest {

    private ResourceEventParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        parser = new ResourceEventParser(objectMapper);
    }

    @Test
    void parse_WithRegisterOfficerRequested_ReturnsCorrectEvent() throws Exception {
        RegisterOfficerRequested event = new RegisterOfficerRequested(
                "BADGE-001", "John", "Doe", "Sergeant", "john.doe@police.gov",
                "555-0100", "2020-01-15", "Active"
        );
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(RegisterOfficerRequested.class);
        RegisterOfficerRequested parsed = (RegisterOfficerRequested) result;
        assertThat(parsed.getBadgeNumber()).isEqualTo("BADGE-001");
        assertThat(parsed.getFirstName()).isEqualTo("John");
    }

    @Test
    void parse_WithUpdateOfficerRequested_ReturnsCorrectEvent() throws Exception {
        UpdateOfficerRequested event = new UpdateOfficerRequested(
                "BADGE-001", null, "Smith", "Lieutenant", null, null, null
        );
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(UpdateOfficerRequested.class);
        UpdateOfficerRequested parsed = (UpdateOfficerRequested) result;
        assertThat(parsed.getBadgeNumber()).isEqualTo("BADGE-001");
        assertThat(parsed.getLastName()).isEqualTo("Smith");
    }

    @Test
    void parse_WithChangeOfficerStatusRequested_ReturnsCorrectEvent() throws Exception {
        ChangeOfficerStatusRequested event = new ChangeOfficerStatusRequested("BADGE-001", "On-Duty");
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(ChangeOfficerStatusRequested.class);
        ChangeOfficerStatusRequested parsed = (ChangeOfficerStatusRequested) result;
        assertThat(parsed.getBadgeNumber()).isEqualTo("BADGE-001");
        assertThat(parsed.getStatus()).isEqualTo("On-Duty");
    }

    @Test
    void parse_WithRegisterVehicleRequested_ReturnsCorrectEvent() throws Exception {
        RegisterVehicleRequested event = new RegisterVehicleRequested(
                "UNIT-001", "Patrol", "ABC-123", "1HGBH41JXMN109186", "Available", "2024-01-15"
        );
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(RegisterVehicleRequested.class);
        RegisterVehicleRequested parsed = (RegisterVehicleRequested) result;
        assertThat(parsed.getUnitId()).isEqualTo("UNIT-001");
        assertThat(parsed.getVehicleType()).isEqualTo("Patrol");
    }

    @Test
    void parse_WithUpdateVehicleRequested_ReturnsCorrectEvent() throws Exception {
        UpdateVehicleRequested event = new UpdateVehicleRequested(
                "UNIT-001", null, "XYZ-789", null, null, "2024-02-15"
        );
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(UpdateVehicleRequested.class);
        UpdateVehicleRequested parsed = (UpdateVehicleRequested) result;
        assertThat(parsed.getUnitId()).isEqualTo("UNIT-001");
    }

    @Test
    void parse_WithChangeVehicleStatusRequested_ReturnsCorrectEvent() throws Exception {
        ChangeVehicleStatusRequested event = new ChangeVehicleStatusRequested("UNIT-001", "In-Use");
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, "commands.vehicle.status");

        assertThat(result).isInstanceOf(ChangeVehicleStatusRequested.class);
        ChangeVehicleStatusRequested parsed = (ChangeVehicleStatusRequested) result;
        assertThat(parsed.getUnitId()).isEqualTo("UNIT-001");
        assertThat(parsed.getStatus()).isEqualTo("In-Use");
    }

    @Test
    void parse_WithCreateUnitRequested_ReturnsCorrectEvent() throws Exception {
        CreateUnitRequested event = new CreateUnitRequested("UNIT-001", "Patrol", "Available");
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(CreateUnitRequested.class);
        CreateUnitRequested parsed = (CreateUnitRequested) result;
        assertThat(parsed.getUnitId()).isEqualTo("UNIT-001");
        assertThat(parsed.getUnitType()).isEqualTo("Patrol");
    }

    @Test
    void parse_WithUpdateUnitRequested_ReturnsCorrectEvent() throws Exception {
        UpdateUnitRequested event = new UpdateUnitRequested("UNIT-001", "SUV", null);
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(UpdateUnitRequested.class);
        UpdateUnitRequested parsed = (UpdateUnitRequested) result;
        assertThat(parsed.getUnitId()).isEqualTo("UNIT-001");
    }

    @Test
    void parse_WithChangeUnitStatusRequested_ReturnsCorrectEvent() throws Exception {
        ChangeUnitStatusRequested event = new ChangeUnitStatusRequested("UNIT-001", "In-Use");
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(ChangeUnitStatusRequested.class);
        ChangeUnitStatusRequested parsed = (ChangeUnitStatusRequested) result;
        assertThat(parsed.getUnitId()).isEqualTo("UNIT-001");
        assertThat(parsed.getStatus()).isEqualTo("In-Use");
    }

    @Test
    void parse_WithRegisterPersonRequested_ReturnsCorrectEvent() throws Exception {
        RegisterPersonRequested event = new RegisterPersonRequested(
                "PERSON-001", "PERSON-001", "John", "Doe", "1990-01-15", "Male", "White", "555-0100"
        );
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(RegisterPersonRequested.class);
        RegisterPersonRequested parsed = (RegisterPersonRequested) result;
        assertThat(parsed.getPersonId()).isEqualTo("PERSON-001");
        assertThat(parsed.getFirstName()).isEqualTo("John");
    }

    @Test
    void parse_WithUpdatePersonRequested_ReturnsCorrectEvent() throws Exception {
        UpdatePersonRequested event = new UpdatePersonRequested(
                "PERSON-001", null, "Smith", null, null, null, "555-0200"
        );
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(UpdatePersonRequested.class);
        UpdatePersonRequested parsed = (UpdatePersonRequested) result;
        assertThat(parsed.getPersonId()).isEqualTo("PERSON-001");
    }

    @Test
    void parse_WithCreateLocationRequested_ReturnsCorrectEvent() throws Exception {
        CreateLocationRequested event = new CreateLocationRequested(
                "LOC-001", "LOC-001", "123 Main St", "Springfield", "IL", "62701",
                "39.7817", "-89.6501", "Address"
        );
        String payload = objectMapper.writeValueAsString(event);

        Object result = parser.parse(payload, null);

        assertThat(result).isInstanceOf(CreateLocationRequested.class);
        CreateLocationRequested parsed = (CreateLocationRequested) result;
        assertThat(parsed.getLocationId()).isEqualTo("LOC-001");
        assertThat(parsed.getAddress()).isEqualTo("123 Main St");
    }

    @Test
    void parse_WithUpdateLocationRequested_ReturnsCorrectEvent() throws Exception {
        UpdateLocationRequested event = new UpdateLocationRequested(
                "LOC-001", null, "Chicago", null, null, null, null, null
        );
        String payload = objectMapper.writeValueAsString(event);

        // UpdateLocationRequested has fewer fields than CreateLocationRequested
        Object result = parser.parse(payload, "commands.location.update");

        assertThat(result).isInstanceOf(UpdateLocationRequested.class);
        UpdateLocationRequested parsed = (UpdateLocationRequested) result;
        assertThat(parsed.getLocationId()).isEqualTo("LOC-001");
    }

    @Test
    void parse_WithInvalidPayload_ThrowsException() {
        String invalidPayload = "{ invalid json }";

        assertThatThrownBy(() -> parser.parse(invalidPayload, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to parse resource event");
    }

    @Test
    void parse_WithUnknownEvent_ThrowsException() {
        String unknownPayload = "{\"unknownField\":\"value\"}";

        assertThatThrownBy(() -> parser.parse(unknownPayload, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
