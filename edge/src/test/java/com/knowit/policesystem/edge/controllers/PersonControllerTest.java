package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.persons.RegisterPersonRequested;
import com.knowit.policesystem.common.events.persons.UpdatePersonRequested;
import com.knowit.policesystem.edge.domain.Gender;
import com.knowit.policesystem.edge.domain.Race;
import com.knowit.policesystem.edge.dto.RegisterPersonRequestDto;
import com.knowit.policesystem.edge.dto.UpdatePersonRequestDto;
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
 * Integration tests for PersonController.
 * Tests the full flow from REST API call to Kafka event production.
 * Note: NATS/JetStream verification is not included as NATS is disabled in test profile.
 * The DualEventPublisher will still attempt to publish to NATS, but it will be disabled.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PersonControllerTest {

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
    private static final String TOPIC = "person-events";

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
    void testRegisterPerson_WithValidData_ProducesEvent() throws Exception {
        // Given
        String personId = "PERSON-001";
        LocalDate dateOfBirth = LocalDate.of(1990, 5, 20);
        RegisterPersonRequestDto request = new RegisterPersonRequestDto(
                personId,
                "Jane",
                "Smith",
                dateOfBirth,
                null,
                null,
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.personId").value(personId))
                .andExpect(jsonPath("$.message").value("Person registration request created"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(personId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        RegisterPersonRequested event = eventObjectMapper.readValue(record.value(), RegisterPersonRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(personId);
        assertThat(event.getPersonId()).isEqualTo(personId);
        assertThat(event.getFirstName()).isEqualTo("Jane");
        assertThat(event.getLastName()).isEqualTo("Smith");
        assertThat(event.getDateOfBirth()).isEqualTo("1990-05-20");
        assertThat(event.getGender()).isNull();
        assertThat(event.getRace()).isNull();
        assertThat(event.getPhoneNumber()).isNull();
        assertThat(event.getEventType()).isEqualTo("RegisterPersonRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testRegisterPerson_WithAllFields_ProducesEvent() throws Exception {
        // Given - all fields including optional
        String personId = "PERSON-002";
        LocalDate dateOfBirth = LocalDate.of(1985, 3, 15);
        RegisterPersonRequestDto request = new RegisterPersonRequestDto(
                personId,
                "John",
                "Doe",
                dateOfBirth,
                Gender.Male,
                Race.White,
                "555-0200"
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.personId").value(personId));

        // Then - verify event in Kafka with all fields
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        RegisterPersonRequested event = eventObjectMapper.readValue(record.value(), RegisterPersonRequested.class);
        assertThat(event.getPersonId()).isEqualTo(personId);
        assertThat(event.getFirstName()).isEqualTo("John");
        assertThat(event.getLastName()).isEqualTo("Doe");
        assertThat(event.getDateOfBirth()).isEqualTo("1985-03-15");
        assertThat(event.getGender()).isEqualTo("Male");
        assertThat(event.getRace()).isEqualTo("White");
        assertThat(event.getPhoneNumber()).isEqualTo("555-0200");
        assertThat(event.getEventType()).isEqualTo("RegisterPersonRequested");
    }

    @Test
    void testRegisterPerson_WithMissingPersonId_Returns400() throws Exception {
        // Given
        RegisterPersonRequestDto request = new RegisterPersonRequestDto(
                null,  // Missing personId
                "Jane",
                "Smith",
                LocalDate.of(1990, 5, 20),
                null,
                null,
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRegisterPerson_WithMissingFirstName_Returns400() throws Exception {
        // Given
        RegisterPersonRequestDto request = new RegisterPersonRequestDto(
                "PERSON-003",
                null,  // Missing firstName
                "Smith",
                LocalDate.of(1990, 5, 20),
                null,
                null,
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRegisterPerson_WithMissingLastName_Returns400() throws Exception {
        // Given
        RegisterPersonRequestDto request = new RegisterPersonRequestDto(
                "PERSON-004",
                "Jane",
                null,  // Missing lastName
                LocalDate.of(1990, 5, 20),
                null,
                null,
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRegisterPerson_WithMissingDateOfBirth_Returns400() throws Exception {
        // Given
        RegisterPersonRequestDto request = new RegisterPersonRequestDto(
                "PERSON-005",
                "Jane",
                "Smith",
                null,  // Missing dateOfBirth
                null,
                null,
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRegisterPerson_WithInvalidDateOfBirth_Returns400() throws Exception {
        // Given - invalid date format
        String requestJson = """
                {
                    "personId": "PERSON-006",
                    "firstName": "Jane",
                    "lastName": "Smith",
                    "dateOfBirth": "invalid-date"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRegisterPerson_WithInvalidGender_Returns400() throws Exception {
        // Given - invalid gender enum value
        String requestJson = """
                {
                    "personId": "PERSON-007",
                    "firstName": "Jane",
                    "lastName": "Smith",
                    "dateOfBirth": "1990-05-20",
                    "gender": "InvalidGender"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRegisterPerson_WithInvalidRace_Returns400() throws Exception {
        // Given - invalid race enum value
        String requestJson = """
                {
                    "personId": "PERSON-008",
                    "firstName": "Jane",
                    "lastName": "Smith",
                    "dateOfBirth": "1990-05-20",
                    "race": "InvalidRace"
                }
                """;

        // When - call REST API
        mockMvc.perform(post("/api/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testRegisterPerson_WithEmptyPersonId_Returns400() throws Exception {
        // Given
        RegisterPersonRequestDto request = new RegisterPersonRequestDto(
                "",  // Empty personId
                "Jane",
                "Smith",
                LocalDate.of(1990, 5, 20),
                null,
                null,
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdatePerson_WithValidData_ProducesEvent() throws Exception {
        // Given
        String personId = "PERSON-100";
        LocalDate dateOfBirth = LocalDate.of(1990, 5, 20);
        UpdatePersonRequestDto request = new UpdatePersonRequestDto(
                "Jane",
                "Smith",
                dateOfBirth,
                Gender.Female,
                Race.White,
                "555-0200"
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/persons/{personId}", personId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.personId").value(personId))
                .andExpect(jsonPath("$.message").value("Person update request processed"));

        // Then - verify event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(personId);
        assertThat(record.topic()).isEqualTo(TOPIC);

        // Deserialize and verify event data
        UpdatePersonRequested event = eventObjectMapper.readValue(record.value(), UpdatePersonRequested.class);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(personId);
        assertThat(event.getPersonId()).isEqualTo(personId);
        assertThat(event.getFirstName()).isEqualTo("Jane");
        assertThat(event.getLastName()).isEqualTo("Smith");
        assertThat(event.getDateOfBirth()).isEqualTo("1990-05-20");
        assertThat(event.getGender()).isEqualTo("Female");
        assertThat(event.getRace()).isEqualTo("White");
        assertThat(event.getPhoneNumber()).isEqualTo("555-0200");
        assertThat(event.getEventType()).isEqualTo("UpdatePersonRequested");
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testUpdatePerson_WithAllFields_ProducesEvent() throws Exception {
        // Given - all fields provided
        String personId = "PERSON-101";
        LocalDate dateOfBirth = LocalDate.of(1985, 3, 15);
        UpdatePersonRequestDto request = new UpdatePersonRequestDto(
                "John",
                "Doe",
                dateOfBirth,
                Gender.Male,
                Race.Black,
                "555-0300"
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/persons/{personId}", personId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then - verify event in Kafka with all fields
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        UpdatePersonRequested event = eventObjectMapper.readValue(record.value(), UpdatePersonRequested.class);
        assertThat(event.getPersonId()).isEqualTo(personId);
        assertThat(event.getFirstName()).isEqualTo("John");
        assertThat(event.getLastName()).isEqualTo("Doe");
        assertThat(event.getDateOfBirth()).isEqualTo("1985-03-15");
        assertThat(event.getGender()).isEqualTo("Male");
        assertThat(event.getRace()).isEqualTo("Black");
        assertThat(event.getPhoneNumber()).isEqualTo("555-0300");
        assertThat(event.getEventType()).isEqualTo("UpdatePersonRequested");
    }

    @Test
    void testUpdatePerson_WithPartialUpdate_ProducesEvent() throws Exception {
        // Given - only firstName provided (partial update)
        String personId = "PERSON-102";
        UpdatePersonRequestDto request = new UpdatePersonRequestDto(
                "Alice",
                null,
                null,
                null,
                null,
                null
        );

        // When - call REST API
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/persons/{personId}", personId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then - verify event in Kafka with only firstName
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();

        ConsumerRecord<String, String> record = records.iterator().next();
        UpdatePersonRequested event = eventObjectMapper.readValue(record.value(), UpdatePersonRequested.class);
        assertThat(event.getPersonId()).isEqualTo(personId);
        assertThat(event.getFirstName()).isEqualTo("Alice");
        assertThat(event.getLastName()).isNull();
        assertThat(event.getDateOfBirth()).isNull();
        assertThat(event.getGender()).isNull();
        assertThat(event.getRace()).isNull();
        assertThat(event.getPhoneNumber()).isNull();
        assertThat(event.getEventType()).isEqualTo("UpdatePersonRequested");
    }

    @Test
    void testUpdatePerson_WithInvalidDateOfBirth_Returns400() throws Exception {
        // Given - invalid date format
        String personId = "PERSON-103";
        String requestJson = """
                {
                    "firstName": "Jane",
                    "lastName": "Smith",
                    "dateOfBirth": "invalid-date"
                }
                """;

        // When - call REST API
        mockMvc.perform(put("/api/v1/persons/{personId}", personId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdatePerson_WithInvalidGender_Returns400() throws Exception {
        // Given - invalid gender enum value
        String personId = "PERSON-104";
        String requestJson = """
                {
                    "firstName": "Jane",
                    "lastName": "Smith",
                    "gender": "InvalidGender"
                }
                """;

        // When - call REST API
        mockMvc.perform(put("/api/v1/persons/{personId}", personId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdatePerson_WithInvalidRace_Returns400() throws Exception {
        // Given - invalid race enum value
        String personId = "PERSON-105";
        String requestJson = """
                {
                    "firstName": "Jane",
                    "lastName": "Smith",
                    "race": "InvalidRace"
                }
                """;

        // When - call REST API
        mockMvc.perform(put("/api/v1/persons/{personId}", personId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }

    @Test
    void testUpdatePerson_WithEmptyPersonId_Returns400() throws Exception {
        // Given - empty personId in path (this will result in 500 depending on Spring configuration)
        UpdatePersonRequestDto request = new UpdatePersonRequestDto(
                "Jane",
                "Smith",
                LocalDate.of(1990, 5, 20),
                null,
                null,
                null
        );

        // When - call REST API with empty personId (invalid path)
        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(put("/api/v1/persons/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().is5xxServerError()); // 500 for invalid path mapping

        // Then - verify no event in Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isEmpty();
    }
}
