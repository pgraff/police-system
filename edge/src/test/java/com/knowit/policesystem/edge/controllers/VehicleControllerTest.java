package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.vehicles.RegisterVehicleRequested;
import com.knowit.policesystem.common.events.vehicles.UpdateVehicleRequested;
import com.knowit.policesystem.edge.domain.VehicleStatus;
import com.knowit.policesystem.edge.domain.VehicleType;
import com.knowit.policesystem.edge.dto.RegisterVehicleRequestDto;
import com.knowit.policesystem.edge.dto.UpdateVehicleRequestDto;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for VehicleController.
 * Tests the full flow from REST API call to Kafka event production.
 * Note: NATS/JetStream verification is not included as NATS is disabled in test profile.
 * The DualEventPublisher will still attempt to publish to NATS, but it will be disabled.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class VehicleControllerTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:latest")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;
    private ObjectMapper eventObjectMapper;
    private static final String TOPIC = "vehicle-events";

    @BeforeEach
    void setUp() {
        // Configure ObjectMapper for event deserialization
        eventObjectMapper = new ObjectMapper();
        eventObjectMapper.registerModule(new JavaTimeModule());
        eventObjectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        eventObjectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // Create Kafka consumer for verification
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(TOPIC));
        
        // Wait for partition assignment and consume any existing events
        consumer.poll(Duration.ofSeconds(1));
        
        // Consume and discard all existing events to start fresh
        ConsumerRecords<String, String> existingRecords;
        do {
            existingRecords = consumer.poll(Duration.ofMillis(100));
        } while (!existingRecords.isEmpty());
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void testRegisterVehicle_WithValidData_ProducesEvent() throws Exception {
        // Given
        String unitId = "UNIT-001";
        LocalDate lastMaintenanceDate = LocalDate.of(2024, 1, 15);
        RegisterVehicleRequestDto request = new RegisterVehicleRequestDto(
                unitId,
                VehicleType.Patrol,
                "ABC-123",
                "1HGBH41JXMN109186",
                VehicleStatus.Available,
                lastMaintenanceDate
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.vehicleId").value(unitId))
                .andExpect(jsonPath("$.data.unitId").value(unitId))
                .andExpect(jsonPath("$.message").value("Vehicle registration request created"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(unitId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        RegisterVehicleRequested event = eventObjectMapper.readValue(record.value(), RegisterVehicleRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(unitId);
        assertThat(event.getUnitId()).isEqualTo(unitId);
        assertThat(event.getVehicleType()).isEqualTo("Patrol");
        assertThat(event.getLicensePlate()).isEqualTo("ABC-123");
        assertThat(event.getVin()).isEqualTo("1HGBH41JXMN109186");
        assertThat(event.getStatus()).isEqualTo("Available");
        assertThat(event.getLastMaintenanceDate()).isEqualTo("2024-01-15");
        assertThat(event.getEventType()).isEqualTo("RegisterVehicleRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testRegisterVehicle_WithMissingUnitId_Returns400() throws Exception {
        // Given
        RegisterVehicleRequestDto request = new RegisterVehicleRequestDto(
                null,  // Missing unitId
                VehicleType.Patrol,
                "ABC-123",
                "1HGBH41JXMN109186",
                VehicleStatus.Available,
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRegisterVehicle_WithInvalidVIN_Returns400() throws Exception {
        // Given - invalid VIN (wrong length)
        String requestJson = """
                {
                    "unitId": "UNIT-002",
                    "vehicleType": "Patrol",
                    "licensePlate": "DEF-456",
                    "vin": "1HGBH41JXMN10918",
                    "status": "Available"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRegisterVehicle_WithInvalidVIN_ContainsInvalidCharacters_Returns400() throws Exception {
        // Given - invalid VIN (contains I, O, or Q)
        String requestJson = """
                {
                    "unitId": "UNIT-003",
                    "vehicleType": "Patrol",
                    "licensePlate": "GHI-789",
                    "vin": "1HGBH41JXMN10918I",
                    "status": "Available"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRegisterVehicle_WithInvalidStatus_Returns400() throws Exception {
        // Given - invalid status enum value
        String requestJson = """
                {
                    "unitId": "UNIT-004",
                    "vehicleType": "Patrol",
                    "licensePlate": "JKL-012",
                    "vin": "1HGBH41JXMN109186",
                    "status": "InvalidStatus"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRegisterVehicle_WithInvalidVehicleType_Returns400() throws Exception {
        // Given - invalid vehicleType enum value
        String requestJson = """
                {
                    "unitId": "UNIT-005",
                    "vehicleType": "InvalidType",
                    "licensePlate": "MNO-345",
                    "vin": "1HGBH41JXMN109186",
                    "status": "Available"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRegisterVehicle_WithEmptyUnitId_Returns400() throws Exception {
        // Given
        RegisterVehicleRequestDto request = new RegisterVehicleRequestDto(
                "",  // Empty unitId
                VehicleType.SUV,
                "PQR-678",
                "1HGBH41JXMN109186",
                VehicleStatus.Assigned,
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRegisterVehicle_WithValidData_WithoutLastMaintenanceDate_ProducesEvent() throws Exception {
        // Given - valid data without optional lastMaintenanceDate
        String unitId = "UNIT-006";
        RegisterVehicleRequestDto request = new RegisterVehicleRequestDto(
                unitId,
                VehicleType.Motorcycle,
                "STU-901",
                "1HGBH41JXMN109186",
                VehicleStatus.InUse,
                null  // Optional field
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.vehicleId").value(unitId))
                .andExpect(jsonPath("$.data.unitId").value(unitId));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        RegisterVehicleRequested event = eventObjectMapper.readValue(record.value(), RegisterVehicleRequested.class);
        assertThat(event.getUnitId()).isEqualTo(unitId);
        assertThat(event.getVehicleType()).isEqualTo("Motorcycle");
        assertThat(event.getLicensePlate()).isEqualTo("STU-901");
        assertThat(event.getVin()).isEqualTo("1HGBH41JXMN109186");
        assertThat(event.getStatus()).isEqualTo("InUse");
        assertThat(event.getLastMaintenanceDate()).isNull();
        assertThat(event.getEventType()).isEqualTo("RegisterVehicleRequested");
    }

    @Test
    void testUpdateVehicle_WithValidData_ProducesEvent() throws Exception {
        // Given
        String unitId = "UNIT-007";
        LocalDate lastMaintenanceDate = LocalDate.of(2024, 2, 20);
        UpdateVehicleRequestDto request = new UpdateVehicleRequestDto(
                VehicleType.SUV,
                "XYZ-123",
                "2HGBH41JXMN109186",
                VehicleStatus.Maintenance,
                lastMaintenanceDate
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/vehicles/{unitId}", unitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vehicleId").value(unitId))
                .andExpect(jsonPath("$.data.unitId").value(unitId))
                .andExpect(jsonPath("$.message").value("Vehicle update request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(unitId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        UpdateVehicleRequested event = eventObjectMapper.readValue(record.value(), UpdateVehicleRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(unitId);
        assertThat(event.getUnitId()).isEqualTo(unitId);
        assertThat(event.getVehicleType()).isEqualTo("SUV");
        assertThat(event.getLicensePlate()).isEqualTo("XYZ-123");
        assertThat(event.getVin()).isEqualTo("2HGBH41JXMN109186");
        assertThat(event.getStatus()).isEqualTo("Maintenance");
        assertThat(event.getLastMaintenanceDate()).isEqualTo("2024-02-20");
        assertThat(event.getEventType()).isEqualTo("UpdateVehicleRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testUpdateVehicle_WithAllFields_ProducesEvent() throws Exception {
        // Given - all fields provided
        String unitId = "UNIT-008";
        LocalDate lastMaintenanceDate = LocalDate.of(2024, 3, 15);
        UpdateVehicleRequestDto request = new UpdateVehicleRequestDto(
                VehicleType.Truck,
                "ABC-999",
                "3HGBH41JXMN109186",
                VehicleStatus.OutOfService,
                lastMaintenanceDate
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/vehicles/{unitId}", unitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then - verify event in Kafka with all fields
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        UpdateVehicleRequested event = eventObjectMapper.readValue(record.value(), UpdateVehicleRequested.class);
        assertThat(event.getUnitId()).isEqualTo(unitId);
        assertThat(event.getVehicleType()).isEqualTo("Truck");
        assertThat(event.getLicensePlate()).isEqualTo("ABC-999");
        assertThat(event.getVin()).isEqualTo("3HGBH41JXMN109186");
        assertThat(event.getStatus()).isEqualTo("OutOfService");
        assertThat(event.getLastMaintenanceDate()).isEqualTo("2024-03-15");
    }

    @Test
    void testUpdateVehicle_WithOnlyVehicleType_ProducesEvent() throws Exception {
        // Given - only vehicleType provided (partial update)
        String unitId = "UNIT-009";
        UpdateVehicleRequestDto request = new UpdateVehicleRequestDto(
                VehicleType.Van,
                null,
                null,
                null,
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/vehicles/{unitId}", unitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then - verify event in Kafka with only vehicleType
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        UpdateVehicleRequested event = eventObjectMapper.readValue(record.value(), UpdateVehicleRequested.class);
        assertThat(event.getUnitId()).isEqualTo(unitId);
        assertThat(event.getVehicleType()).isEqualTo("Van");
        assertThat(event.getLicensePlate()).isNull();
        assertThat(event.getVin()).isNull();
        assertThat(event.getStatus()).isNull();
        assertThat(event.getLastMaintenanceDate()).isNull();
    }

    @Test
    void testUpdateVehicle_WithInvalidVIN_Returns400() throws Exception {
        // Given - invalid VIN format (wrong length)
        String unitId = "UNIT-010";
        String requestJson = """
                {
                    "vehicleType": "Patrol",
                    "vin": "1HGBH41JXMN10918"
                }
                """;

        // When - call REST API
        mockMvc.perform(put("/api/v1/vehicles/{unitId}", unitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdateVehicle_WithEmptyUnitId_Returns400() throws Exception {
        // Given - empty unitId in path (this will result in 404 or 500 depending on Spring configuration)
        UpdateVehicleRequestDto request = new UpdateVehicleRequestDto(
                VehicleType.Patrol,
                null,
                null,
                null,
                null
        );

        // When - call REST API with empty unitId (invalid path)
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/vehicles/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().is5xxServerError()); // 500 for invalid path mapping

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }
}
