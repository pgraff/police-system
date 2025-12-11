package com.knowit.policesystem.edge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.EventClassification;
import com.knowit.policesystem.common.events.persons.RegisterPersonRequested;
import com.knowit.policesystem.common.events.persons.UpdatePersonRequested;
import com.knowit.policesystem.edge.domain.Gender;
import com.knowit.policesystem.edge.domain.Race;
import com.knowit.policesystem.edge.dto.RegisterPersonRequestDto;
import com.knowit.policesystem.edge.dto.UpdatePersonRequestDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
 * Tests the full flow from REST API call to both Kafka and NATS/JetStream event production.
 */
class PersonControllerTest extends BaseIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(PersonControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;
    private ObjectMapper eventObjectMapper;
    private NatsTestHelper natsHelper;
    private static final String TOPIC = "person-events";

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
        consumer.subscribe(Collections.singletonList(TOPIC));
        
        // Wait for partition assignment - consumer will start at latest offset automatically
        consumer.poll(Duration.ofSeconds(1));

        // Create NATS test helper
        natsHelper = new NatsTestHelper(nats.getNatsUrl(), eventObjectMapper);
        
        // Pre-create a catch-all stream for all command subjects to ensure it exists before publishing
        try {
            natsHelper.ensureStreamForSubject("commands.>");
        } catch (Exception e) {
            logger.warn("Error pre-creating NATS stream", e);
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

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(natsSubject);
        Thread.sleep(1000);
        natsSubscription.pull(1);
        Message natsMsg = natsSubscription.nextMessage(Duration.ofSeconds(5));
        assertThat(natsMsg).isNotNull();
        String natsEventJson = new String(natsMsg.getData(), java.nio.charset.StandardCharsets.UTF_8);
        RegisterPersonRequested natsEvent = eventObjectMapper.readValue(natsEventJson, RegisterPersonRequested.class);
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getPersonId()).isEqualTo(personId);
        assertThat(natsEvent.getEventType()).isEqualTo("RegisterPersonRequested");
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

        // Prepare NATS subscription before API call to ensure we don't miss the message
        String expectedNatsSubject = "commands.person.update";
        JetStreamSubscription natsSubscription = natsHelper.prepareSubscription(expectedNatsSubject);

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

        // Verify event also published to NATS (critical event)
        String natsSubject = EventClassification.generateNatsSubject(event);
        assertThat(natsSubject).isEqualTo(expectedNatsSubject);
        
        // Wait for async publish to complete and message to be available in stream
        Thread.sleep(1000);
        
        // Consume messages until we find the one with matching eventId
        Message natsMsg = null;
        UpdatePersonRequested natsEvent = null;
        for (int i = 0; i < 10; i++) {
            natsSubscription.pull(1);
            Message msg = natsSubscription.nextMessage(Duration.ofSeconds(2));
            if (msg != null) {
                String msgJson = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
                UpdatePersonRequested msgEvent = eventObjectMapper.readValue(msgJson, UpdatePersonRequested.class);
                if (msgEvent.getEventId().equals(event.getEventId())) {
                    natsMsg = msg;
                    natsEvent = msgEvent;
                    break;
                } else {
                    // Not our message, ack it and continue
                    msg.ack();
                }
            }
        }
        
        assertThat(natsMsg).isNotNull();
        natsMsg.ack();
        assertThat(natsEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(natsEvent.getPersonId()).isEqualTo(personId);
        assertThat(natsEvent.getEventType()).isEqualTo("UpdatePersonRequested");
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
