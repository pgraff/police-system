package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.EventClassification;
import com.knowit.policesystem.common.events.locations.CreateLocationRequested;
import com.knowit.policesystem.common.events.locations.LinkLocationToIncidentRequested;
import com.knowit.policesystem.common.events.locations.LinkLocationToCallRequested;
import com.knowit.policesystem.common.events.locations.UnlinkLocationFromIncidentRequested;
import com.knowit.policesystem.common.events.locations.UnlinkLocationFromCallRequested;
import com.knowit.policesystem.common.events.locations.UpdateLocationRequested;
import com.knowit.policesystem.edge.domain.LocationRoleType;
import com.knowit.policesystem.edge.domain.LocationType;
import com.knowit.policesystem.edge.dto.CreateLocationRequestDto;
import com.knowit.policesystem.edge.dto.LinkLocationRequestDto;
import com.knowit.policesystem.edge.dto.UpdateLocationRequestDto;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.infrastructure.BaseIntegrationTest;
import com.knowit.policesystem.edge.infrastructure.NatsTestHelper;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for LocationController.
 * Tests the full flow from REST API call to both Kafka and NATS/JetStream event production.
 */
class LocationControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TopicConfiguration topicConfiguration;

    private Consumer<String, String> consumer;
    private ObjectMapper eventObjectMapper;
    private NatsTestHelper natsHelper;

    @BeforeEach
    void setUp() throws Exception {
        // Configure ObjectMapper for event deserialization
        eventObjectMapper = new ObjectMapper();
        eventObjectMapper.registerModule(new JavaTimeModule());
        eventObjectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        eventObjectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // Create Kafka consumer for verification
        // Use "latest" offset to only receive new messages published after subscription
        // This avoids conflicts with shared containers and is much faster
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(topicConfiguration.LOCATION_EVENTS));

        // Wait for partition assignment - consumer will start at latest offset automatically
        consumer.poll(Duration.ofSeconds(1));

        // Create NATS test helper
        natsHelper = new NatsTestHelper(nats.getNatsUrl(), eventObjectMapper);
        
        // Pre-create a catch-all stream for all command subjects to ensure it exists before publishing
        try {
            natsHelper.ensureStreamForSubject("commands.>");
        } catch (Exception e) {
            // Stream may already exist, continue
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (consumer != null) {
            consumer.close();
        }
        if (natsHelper != null) {
            natsHelper.close();
        }
    }

    @Test
    void testCreateLocation_WithValidData_ProducesEvent() throws Exception {
        // Given
        String locationId = "LOC-001";
        CreateLocationRequestDto request = new CreateLocationRequestDto(
                locationId,
                "123 Main St",
                "Springfield",
                "IL",
                "62701",
                39.7817,
                -89.6501,
                LocationType.Street
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.locationId").value(locationId))
                .andExpect(jsonPath("$.message").value("Location creation request created"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(locationId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.LOCATION_EVENTS);

        // Deserialize and verify event data
        CreateLocationRequested event = eventObjectMapper.readValue(record.value(), CreateLocationRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(locationId);
        assertThat(event.getLocationId()).isEqualTo(locationId);
        assertThat(event.getAddress()).isEqualTo("123 Main St");
        assertThat(event.getCity()).isEqualTo("Springfield");
        assertThat(event.getState()).isEqualTo("IL");
        assertThat(event.getZipCode()).isEqualTo("62701");
        assertThat(event.getLatitude()).isEqualTo("39.7817");
        assertThat(event.getLongitude()).isEqualTo("-89.6501");
        assertThat(event.getLocationType()).isEqualTo("Street");
        assertThat(event.getEventType()).isEqualTo("CreateLocationRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        // Consume messages until we find the one with matching eventId
        Message natsMsg = null;
        CreateLocationRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                CreateLocationRequested msgEvent = eventObjectMapper.readValue(msgJson, CreateLocationRequested.class);
                if (msgEvent.getEventId().equals(event.getEventId())) {
                    natsMsg = msg;
                    natsEvent = msgEvent;
                    break;
                } else {
                    msg.ack();
                }
            }
        }
        
        assertThat(natsMsg).isNotNull();
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getLocationId()).isEqualTo(locationId);
        assertThat(natsEvent.getEventType()).isEqualTo("CreateLocationRequested");
    }

    @Test
    void testCreateLocation_WithMinimalData_ProducesEvent() throws Exception {
        // Given - only required fields
        String locationId = "LOC-002";
        CreateLocationRequestDto request = new CreateLocationRequestDto(
                locationId,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.locationId").value(locationId));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        CreateLocationRequested event = eventObjectMapper.readValue(record.value(), CreateLocationRequested.class);
        assertThat(event.getLocationId()).isEqualTo(locationId);
        assertThat(event.getAddress()).isNull();
        assertThat(event.getCity()).isNull();
        assertThat(event.getState()).isNull();
        assertThat(event.getZipCode()).isNull();
        assertThat(event.getLatitude()).isNull();
        assertThat(event.getLongitude()).isNull();
        assertThat(event.getLocationType()).isNull();
        assertThat(event.getEventType()).isEqualTo("CreateLocationRequested");
    }

    @Test
    void testCreateLocation_WithMissingLocationId_Returns400() throws Exception {
        // Given
        CreateLocationRequestDto request = new CreateLocationRequestDto(
                null,  // Missing locationId
                "123 Main St",
                "Springfield",
                "IL",
                "62701",
                39.7817,
                -89.6501,
                LocationType.Street
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCreateLocation_WithEmptyLocationId_Returns400() throws Exception {
        // Given
        CreateLocationRequestDto request = new CreateLocationRequestDto(
                "",  // Empty locationId
                "123 Main St",
                "Springfield",
                "IL",
                "62701",
                39.7817,
                -89.6501,
                LocationType.Street
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCreateLocation_WithLatitudeGreaterThan90_Returns400() throws Exception {
        // Given - latitude > 90
        String locationId = "LOC-003";
        CreateLocationRequestDto request = new CreateLocationRequestDto(
                locationId,
                "123 Main St",
                "Springfield",
                "IL",
                "62701",
                91.0,  // Invalid latitude
                -89.6501,
                LocationType.Street
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCreateLocation_WithLatitudeLessThanNegative90_Returns400() throws Exception {
        // Given - latitude < -90
        String locationId = "LOC-004";
        CreateLocationRequestDto request = new CreateLocationRequestDto(
                locationId,
                "123 Main St",
                "Springfield",
                "IL",
                "62701",
                -91.0,  // Invalid latitude
                -89.6501,
                LocationType.Street
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCreateLocation_WithLongitudeGreaterThan180_Returns400() throws Exception {
        // Given - longitude > 180
        String locationId = "LOC-005";
        CreateLocationRequestDto request = new CreateLocationRequestDto(
                locationId,
                "123 Main St",
                "Springfield",
                "IL",
                "62701",
                39.7817,
                181.0,  // Invalid longitude
                LocationType.Street
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCreateLocation_WithLongitudeLessThanNegative180_Returns400() throws Exception {
        // Given - longitude < -180
        String locationId = "LOC-006";
        CreateLocationRequestDto request = new CreateLocationRequestDto(
                locationId,
                "123 Main St",
                "Springfield",
                "IL",
                "62701",
                39.7817,
                -181.0,  // Invalid longitude
                LocationType.Street
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCreateLocation_WithInvalidLocationType_Returns400() throws Exception {
        // Given - invalid locationType enum value
        String requestJson = """
                {
                    "locationId": "LOC-007",
                    "address": "123 Main St",
                    "city": "Springfield",
                    "state": "IL",
                    "zipCode": "62701",
                    "latitude": 39.7817,
                    "longitude": -89.6501,
                    "locationType": "InvalidLocationType"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testCreateLocation_WithValidCoordinatesAtBoundaries_ProducesEvent() throws Exception {
        // Given - coordinates at valid boundaries
        String locationId = "LOC-008";
        CreateLocationRequestDto request = new CreateLocationRequestDto(
                locationId,
                "123 Main St",
                "Springfield",
                "IL",
                "62701",
                90.0,   // Maximum valid latitude
                180.0,  // Maximum valid longitude
                LocationType.Street
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        CreateLocationRequested event = eventObjectMapper.readValue(record.value(), CreateLocationRequested.class);
        assertThat(event.getLocationId()).isEqualTo(locationId);
        assertThat(event.getLatitude()).isEqualTo("90.0");
        assertThat(event.getLongitude()).isEqualTo("180.0");
    }

    @Test
    void testCreateLocation_WithValidCoordinatesAtNegativeBoundaries_ProducesEvent() throws Exception {
        // Given - coordinates at valid negative boundaries
        String locationId = "LOC-009";
        CreateLocationRequestDto request = new CreateLocationRequestDto(
                locationId,
                "123 Main St",
                "Springfield",
                "IL",
                "62701",
                -90.0,   // Minimum valid latitude
                -180.0,  // Minimum valid longitude
                LocationType.Street
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        CreateLocationRequested event = eventObjectMapper.readValue(record.value(), CreateLocationRequested.class);
        assertThat(event.getLocationId()).isEqualTo(locationId);
        assertThat(event.getLatitude()).isEqualTo("-90.0");
        assertThat(event.getLongitude()).isEqualTo("-180.0");
    }

    @Test
    void testUpdateLocation_WithValidData_ProducesEvent() throws Exception {
        // Given
        String locationId = "LOC-100";
        UpdateLocationRequestDto request = new UpdateLocationRequestDto(
                "456 Oak Ave",
                "Chicago",
                "IL",
                "60601",
                41.8781,
                -87.6298,
                LocationType.Building
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/locations/{locationId}", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationId").value(locationId))
                .andExpect(jsonPath("$.message").value("Location update request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(locationId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.LOCATION_EVENTS);

        // Deserialize and verify event data
        UpdateLocationRequested event = eventObjectMapper.readValue(record.value(), UpdateLocationRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(locationId);
        assertThat(event.getLocationId()).isEqualTo(locationId);
        assertThat(event.getAddress()).isEqualTo("456 Oak Ave");
        assertThat(event.getCity()).isEqualTo("Chicago");
        assertThat(event.getState()).isEqualTo("IL");
        assertThat(event.getZipCode()).isEqualTo("60601");
        assertThat(event.getLatitude()).isEqualTo("41.8781");
        assertThat(event.getLongitude()).isEqualTo("-87.6298");
        assertThat(event.getLocationType()).isEqualTo("Building");
        assertThat(event.getEventType()).isEqualTo("UpdateLocationRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        // Consume messages until we find the one with matching eventId
        Message natsMsg = null;
        UpdateLocationRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                UpdateLocationRequested msgEvent = eventObjectMapper.readValue(msgJson, UpdateLocationRequested.class);
                if (msgEvent.getEventId().equals(event.getEventId())) {
                    natsMsg = msg;
                    natsEvent = msgEvent;
                    break;
                } else {
                    msg.ack();
                }
            }
        }
        
        assertThat(natsMsg).isNotNull();
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getLocationId()).isEqualTo(locationId);
        assertThat(natsEvent.getEventType()).isEqualTo("UpdateLocationRequested");
    }

    @Test
    void testUpdateLocation_WithPartialUpdate_ProducesEvent() throws Exception {
        // Given - only address and city provided
        String locationId = "LOC-101";
        UpdateLocationRequestDto request = new UpdateLocationRequestDto(
                "789 Pine St",
                "Boston",
                null,
                null,
                null,
                null,
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/locations/{locationId}", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationId").value(locationId));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        UpdateLocationRequested event = eventObjectMapper.readValue(record.value(), UpdateLocationRequested.class);
        assertThat(event.getLocationId()).isEqualTo(locationId);
        assertThat(event.getAddress()).isEqualTo("789 Pine St");
        assertThat(event.getCity()).isEqualTo("Boston");
        assertThat(event.getState()).isNull();
        assertThat(event.getZipCode()).isNull();
        assertThat(event.getLatitude()).isNull();
        assertThat(event.getLongitude()).isNull();
        assertThat(event.getLocationType()).isNull();
        assertThat(event.getEventType()).isEqualTo("UpdateLocationRequested");
    }

    @Test
    void testUpdateLocation_WithLatitudeGreaterThan90_Returns400() throws Exception {
        // Given - latitude > 90
        String locationId = "LOC-102";
        UpdateLocationRequestDto request = new UpdateLocationRequestDto(
                "123 Main St",
                "Springfield",
                "IL",
                "62701",
                91.0,  // Invalid latitude
                -89.6501,
                LocationType.Street
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/locations/{locationId}", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdateLocation_WithLatitudeLessThanNegative90_Returns400() throws Exception {
        // Given - latitude < -90
        String locationId = "LOC-103";
        UpdateLocationRequestDto request = new UpdateLocationRequestDto(
                "123 Main St",
                "Springfield",
                "IL",
                "62701",
                -91.0,  // Invalid latitude
                -89.6501,
                LocationType.Street
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/locations/{locationId}", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdateLocation_WithLongitudeGreaterThan180_Returns400() throws Exception {
        // Given - longitude > 180
        String locationId = "LOC-104";
        UpdateLocationRequestDto request = new UpdateLocationRequestDto(
                "123 Main St",
                "Springfield",
                "IL",
                "62701",
                39.7817,
                181.0,  // Invalid longitude
                LocationType.Street
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/locations/{locationId}", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdateLocation_WithLongitudeLessThanNegative180_Returns400() throws Exception {
        // Given - longitude < -180
        String locationId = "LOC-105";
        UpdateLocationRequestDto request = new UpdateLocationRequestDto(
                "123 Main St",
                "Springfield",
                "IL",
                "62701",
                39.7817,
                -181.0,  // Invalid longitude
                LocationType.Street
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/locations/{locationId}", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdateLocation_WithInvalidLocationType_Returns400() throws Exception {
        // Given - invalid locationType enum value
        String locationId = "LOC-106";
        String requestJson = """
                {
                    "address": "123 Main St",
                    "city": "Springfield",
                    "state": "IL",
                    "zipCode": "62701",
                    "latitude": 39.7817,
                    "longitude": -89.6501,
                    "locationType": "InvalidLocationType"
                }
                """;

        // When - call REST API
        mockMvc.perform(put("/api/v1/locations/{locationId}", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdateLocation_WithValidCoordinatesAtBoundaries_ProducesEvent() throws Exception {
        // Given - coordinates at valid boundaries
        String locationId = "LOC-107";
        UpdateLocationRequestDto request = new UpdateLocationRequestDto(
                "123 Main St",
                "Springfield",
                "IL",
                "62701",
                90.0,   // Maximum valid latitude
                180.0,  // Maximum valid longitude
                LocationType.Street
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/locations/{locationId}", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        UpdateLocationRequested event = eventObjectMapper.readValue(record.value(), UpdateLocationRequested.class);
        assertThat(event.getLocationId()).isEqualTo(locationId);
        assertThat(event.getLatitude()).isEqualTo("90.0");
        assertThat(event.getLongitude()).isEqualTo("180.0");
    }

    @Test
    void testLinkLocationToIncident_WithValidData_ProducesEvent() throws Exception {
        // Given
        String incidentId = "INC-001";
        String locationId = "LOC-001";
        LinkLocationRequestDto request = new LinkLocationRequestDto(
                locationId,
                LocationRoleType.Primary,
                "Primary incident location"
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/locations", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationId").value(locationId))
                .andExpect(jsonPath("$.message").value("Location link request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(locationId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.LOCATION_EVENTS);

        // Deserialize and verify event data
        LinkLocationToIncidentRequested event = eventObjectMapper.readValue(record.value(), LinkLocationToIncidentRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(locationId);
        assertThat(event.getIncidentId()).isEqualTo(incidentId);
        assertThat(event.getLocationId()).isEqualTo(locationId);
        assertThat(event.getLocationRoleType()).isEqualTo("Primary");
        assertThat(event.getDescription()).isEqualTo("Primary incident location");
        assertThat(event.getEventType()).isEqualTo("LinkLocationToIncidentRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        // Consume messages until we find the one with matching eventId
        Message natsMsg = null;
        LinkLocationToIncidentRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                LinkLocationToIncidentRequested msgEvent = eventObjectMapper.readValue(msgJson, LinkLocationToIncidentRequested.class);
                if (msgEvent.getEventId().equals(event.getEventId())) {
                    natsMsg = msg;
                    natsEvent = msgEvent;
                    break;
                } else {
                    msg.ack();
                }
            }
        }
        
        assertThat(natsMsg).isNotNull();
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getLocationId()).isEqualTo(locationId);
        assertThat(natsEvent.getEventType()).isEqualTo("LinkLocationToIncidentRequested");
    }

    @Test
    void testLinkLocationToIncident_WithMissingLocationId_Returns400() throws Exception {
        // Given
        String incidentId = "INC-001";
        LinkLocationRequestDto request = new LinkLocationRequestDto(
                null,  // Missing locationId
                LocationRoleType.Primary,
                "Primary incident location"
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/locations", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testLinkLocationToIncident_WithMissingLocationRoleType_Returns400() throws Exception {
        // Given
        String incidentId = "INC-001";
        String locationId = "LOC-001";
        LinkLocationRequestDto request = new LinkLocationRequestDto(
                locationId,
                null,  // Missing locationRoleType
                "Primary incident location"
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/locations", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testLinkLocationToIncident_WithInvalidLocationRoleType_Returns400() throws Exception {
        // Given - invalid enum value (Jackson will reject this before validation)
        String incidentId = "INC-001";
        String requestJson = "{\"locationId\":\"LOC-001\",\"locationRoleType\":\"InvalidRole\",\"description\":\"Test\"}";

        // When - call REST API
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/locations", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testLinkLocationToIncident_WithEmptyLocationId_Returns400() throws Exception {
        // Given
        String incidentId = "INC-001";
        LinkLocationRequestDto request = new LinkLocationRequestDto(
                "",  // Empty locationId
                LocationRoleType.Primary,
                "Primary incident location"
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/locations", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testLinkLocationToIncident_WithAllLocationRoleTypes_ProducesEvent() throws Exception {
        // Given - test all enum values
        String incidentId = "INC-001";
        String[] roleTypes = {"Primary", "Secondary", "Related", "Other"};

        for (String roleType : roleTypes) {
            String locationId = "LOC-" + roleType;
            LinkLocationRequestDto request = new LinkLocationRequestDto(
                    locationId,
                    LocationRoleType.valueOf(roleType),
                    "Test " + roleType + " location"
            );

            // When - call REST API
            String requestJson = objectMapper.writeValueAsString(request);
            mockMvc.perform(post("/api/v1/incidents/{incidentId}/locations", incidentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());

            // Then - verify event in Kafka
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            assertThat(records).isNotEmpty();

            ConsumerRecord<String, String> record = records.iterator().next();
            LinkLocationToIncidentRequested event = eventObjectMapper.readValue(record.value(), LinkLocationToIncidentRequested.class);
            assertThat(event.getLocationRoleType()).isEqualTo(roleType);
            assertThat(event.getLocationId()).isEqualTo(locationId);
        }
    }

    @Test
    void testLinkLocationToIncident_WithOptionalDescription_ProducesEvent() throws Exception {
        // Given - description is optional
        String incidentId = "INC-001";
        String locationId = "LOC-001";
        LinkLocationRequestDto request = new LinkLocationRequestDto(
                locationId,
                LocationRoleType.Secondary,
                null  // Optional description
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/locations", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        LinkLocationToIncidentRequested event = eventObjectMapper.readValue(record.value(), LinkLocationToIncidentRequested.class);
        assertThat(event.getLocationId()).isEqualTo(locationId);
        assertThat(event.getIncidentId()).isEqualTo(incidentId);
        assertThat(event.getLocationRoleType()).isEqualTo("Secondary");
        assertThat(event.getDescription()).isNull();
    }

    @Test
    void testLinkLocationToCall_WithValidData_ProducesEvent() throws Exception {
        // Given
        String callId = "CALL-001";
        String locationId = "LOC-100";
        LinkLocationRequestDto request = new LinkLocationRequestDto(
                locationId,
                LocationRoleType.Primary,
                "Primary call location"
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls/{callId}/locations", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationId").value(locationId))
                .andExpect(jsonPath("$.message").value("Location link request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(locationId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.LOCATION_EVENTS);

        // Deserialize and verify event data
        LinkLocationToCallRequested event = eventObjectMapper.readValue(record.value(), LinkLocationToCallRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(locationId);
        assertThat(event.getCallId()).isEqualTo(callId);
        assertThat(event.getLocationId()).isEqualTo(locationId);
        assertThat(event.getLocationRoleType()).isEqualTo("Primary");
        assertThat(event.getDescription()).isEqualTo("Primary call location");
        assertThat(event.getEventType()).isEqualTo("LinkLocationToCallRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        // Consume messages until we find the one with matching eventId
        Message natsMsg = null;
        LinkLocationToCallRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                LinkLocationToCallRequested msgEvent = eventObjectMapper.readValue(msgJson, LinkLocationToCallRequested.class);
                if (msgEvent.getEventId().equals(event.getEventId())) {
                    natsMsg = msg;
                    natsEvent = msgEvent;
                    break;
                } else {
                    msg.ack();
                }
            }
        }
        
        assertThat(natsMsg).isNotNull();
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getLocationId()).isEqualTo(locationId);
        assertThat(natsEvent.getEventType()).isEqualTo("LinkLocationToCallRequested");
    }

    @Test
    void testLinkLocationToCall_WithEmptyCallId_Returns400() throws Exception {
        // Given - whitespace path variable should fail validation
        String callId = " ";
        LinkLocationRequestDto request = new LinkLocationRequestDto(
                "LOC-101",
                LocationRoleType.Primary,
                "Primary call location"
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls/{callId}/locations", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testLinkLocationToCall_WithMissingLocationId_Returns400() throws Exception {
        // Given
        String callId = "CALL-002";
        LinkLocationRequestDto request = new LinkLocationRequestDto(
                null,
                LocationRoleType.Primary,
                "Primary call location"
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls/{callId}/locations", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testLinkLocationToCall_WithMissingLocationRoleType_Returns400() throws Exception {
        // Given
        String callId = "CALL-003";
        LinkLocationRequestDto request = new LinkLocationRequestDto(
                "LOC-102",
                null,
                "Primary call location"
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/calls/{callId}/locations", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testLinkLocationToCall_WithInvalidLocationRoleType_Returns400() throws Exception {
        // Given - invalid enum value (Jackson will reject this before validation)
        String callId = "CALL-004";
        String requestJson = "{\"locationId\":\"LOC-103\",\"locationRoleType\":\"InvalidRole\",\"description\":\"Test\"}";

        // When - call REST API
        mockMvc.perform(post("/api/v1/calls/{callId}/locations", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUnlinkLocationFromIncident_WithValidData_ProducesEvent() throws Exception {
        // Given
        String incidentId = "INC-001";
        String locationId = "LOC-001";

        // When - call REST API
        mockMvc.perform(delete("/api/v1/incidents/{incidentId}/locations/{locationId}", incidentId, locationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationId").value(locationId))
                .andExpect(jsonPath("$.message").value("Location unlink request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(locationId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.LOCATION_EVENTS);

        // Deserialize and verify event data
        UnlinkLocationFromIncidentRequested event = eventObjectMapper.readValue(record.value(), UnlinkLocationFromIncidentRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(locationId);
        assertThat(event.getIncidentId()).isEqualTo(incidentId);
        assertThat(event.getLocationId()).isEqualTo(locationId);
        assertThat(event.getEventType()).isEqualTo("UnlinkLocationFromIncidentRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        // Consume messages until we find the one with matching eventId
        Message natsMsg = null;
        UnlinkLocationFromIncidentRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                UnlinkLocationFromIncidentRequested msgEvent = eventObjectMapper.readValue(msgJson, UnlinkLocationFromIncidentRequested.class);
                if (msgEvent.getEventId().equals(event.getEventId())) {
                    natsMsg = msg;
                    natsEvent = msgEvent;
                    break;
                } else {
                    msg.ack();
                }
            }
        }
        
        assertThat(natsMsg).isNotNull();
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getLocationId()).isEqualTo(locationId);
        assertThat(natsEvent.getEventType()).isEqualTo("UnlinkLocationFromIncidentRequested");
    }

    @Test
    void testUnlinkLocationFromIncident_WithEmptyIncidentId_Returns400() throws Exception {
        // Given - whitespace-only incidentId path parameter (empty string causes 500, so use whitespace)
        String incidentId = "   ";
        String locationId = "LOC-001";

        // When - call REST API
        mockMvc.perform(delete("/api/v1/incidents/{incidentId}/locations/{locationId}", incidentId, locationId))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUnlinkLocationFromIncident_WithEmptyLocationId_Returns400() throws Exception {
        // Given - whitespace-only locationId path parameter (empty string causes 500, so use whitespace)
        String incidentId = "INC-001";
        String locationId = "   ";

        // When - call REST API
        mockMvc.perform(delete("/api/v1/incidents/{incidentId}/locations/{locationId}", incidentId, locationId))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUnlinkLocationFromCall_WithValidData_ProducesEvent() throws Exception {
        // Given
        String callId = "CALL-001";
        String locationId = "LOC-201";

        // When - call REST API
        mockMvc.perform(delete("/api/v1/calls/{callId}/locations/{locationId}", callId, locationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationId").value(locationId))
                .andExpect(jsonPath("$.message").value("Location unlink request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(locationId);
        assertThat(record.topic()).isEqualTo(topicConfiguration.LOCATION_EVENTS);

        // Deserialize and verify event data
        UnlinkLocationFromCallRequested event = eventObjectMapper.readValue(record.value(), UnlinkLocationFromCallRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(locationId);
        assertThat(event.getCallId()).isEqualTo(callId);
        assertThat(event.getLocationId()).isEqualTo(locationId);
        assertThat(event.getEventType()).isEqualTo("UnlinkLocationFromCallRequested");
        assertThat(event.getVersion()).isEqualTo(1);

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        
        // Consume messages until we find the one with matching eventId
        Message natsMsg = null;
        UnlinkLocationFromCallRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                UnlinkLocationFromCallRequested msgEvent = eventObjectMapper.readValue(msgJson, UnlinkLocationFromCallRequested.class);
                if (msgEvent.getEventId().equals(event.getEventId())) {
                    natsMsg = msg;
                    natsEvent = msgEvent;
                    break;
                } else {
                    msg.ack();
                }
            }
        }
        
        assertThat(natsMsg).isNotNull();
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getLocationId()).isEqualTo(locationId);
        assertThat(natsEvent.getEventType()).isEqualTo("UnlinkLocationFromCallRequested");
    }

    @Test
    void testUnlinkLocationFromCall_WithEmptyCallId_Returns400() throws Exception {
        // Given - whitespace-only callId path parameter
        String callId = "   ";
        String locationId = "LOC-201";

        // When - call REST API
        mockMvc.perform(delete("/api/v1/calls/{callId}/locations/{locationId}", callId, locationId))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUnlinkLocationFromCall_WithEmptyLocationId_Returns400() throws Exception {
        // Given - whitespace-only locationId path parameter
        String callId = "CALL-001";
        String locationId = "   ";

        // When - call REST API
        mockMvc.perform(delete("/api/v1/calls/{callId}/locations/{locationId}", callId, locationId))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }
}
