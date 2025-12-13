package com.knowit.policesystem.projection.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.officershifts.CheckInOfficerRequested;
import com.knowit.policesystem.common.events.officershifts.CheckOutOfficerRequested;
import com.knowit.policesystem.common.events.officershifts.UpdateOfficerShiftRequested;
import com.knowit.policesystem.common.events.shifts.ChangeShiftStatusRequested;
import com.knowit.policesystem.common.events.shifts.EndShiftRequested;
import com.knowit.policesystem.common.events.shifts.RecordShiftChangeRequested;
import com.knowit.policesystem.common.events.shifts.StartShiftRequested;
import com.knowit.policesystem.projection.api.OfficerShiftProjectionResponse;
import com.knowit.policesystem.projection.api.ShiftChangeProjectionResponse;
import com.knowit.policesystem.projection.api.ShiftProjectionResponse;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class WorkforceProjectionE2ETest extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper;
    private Properties producerProps;
    private Producer<String, String> producer;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/projections";
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(producerProps);
    }

    @AfterEach
    void tearDown() {
        if (producer != null) {
            producer.close();
        }
        // Clean up test data
        jdbcTemplate.update("DELETE FROM shift_status_history");
        jdbcTemplate.update("DELETE FROM shift_change_projection");
        jdbcTemplate.update("DELETE FROM officer_shift_projection");
        jdbcTemplate.update("DELETE FROM shift_projection");
    }

    @Test
    void eventFlow_StartShiftRequested_KafkaToProjectionToApi_Works() throws Exception {
        String shiftId = "SHIFT-" + UUID.randomUUID();
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(28800); // 8 hours
        StartShiftRequested event = new StartShiftRequested(
                shiftId, startTime, endTime, "Day", "Active"
        );

        String eventJson = objectMapper.writeValueAsString(event);
        producer.send(new ProducerRecord<>("shift-events", shiftId, eventJson));
        producer.flush();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<ShiftProjectionResponse> response = restTemplate.getForEntity(
                            baseUrl + "/shifts/{id}", ShiftProjectionResponse.class, shiftId);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().shiftId()).isEqualTo(shiftId);
                    assertThat(response.getBody().shiftType()).isEqualTo("Day");
                    assertThat(response.getBody().status()).isEqualTo("Active");
                });
    }

    @Test
    void eventFlow_EndShiftRequested_KafkaToProjectionToApi_Works() throws Exception {
        String shiftId = "SHIFT-" + UUID.randomUUID();
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(28800);
        
        // First create the shift
        StartShiftRequested startEvent = new StartShiftRequested(shiftId, startTime, endTime, "Day", "Active");
        producer.send(new ProducerRecord<>("shift-events", shiftId, objectMapper.writeValueAsString(startEvent)));
        producer.flush();

        // Wait for shift to be created
        Awaitility.await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<?> response = restTemplate.getForEntity(
                            baseUrl + "/shifts/{id}", Object.class, shiftId);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                });

        // Then end the shift
        Instant actualEndTime = Instant.now();
        EndShiftRequested endEvent = new EndShiftRequested(shiftId, actualEndTime);
        producer.send(new ProducerRecord<>("shift-events", shiftId, objectMapper.writeValueAsString(endEvent)));
        producer.flush();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<ShiftProjectionResponse> response = restTemplate.getForEntity(
                            baseUrl + "/shifts/{id}", ShiftProjectionResponse.class, shiftId);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().endTime()).isNotNull();
                });
    }

    @Test
    void eventFlow_ChangeShiftStatusRequested_KafkaToProjectionToApi_Works() throws Exception {
        String shiftId = "SHIFT-" + UUID.randomUUID();
        Instant startTime = Instant.now();
        
        // First create the shift
        StartShiftRequested startEvent = new StartShiftRequested(shiftId, startTime, null, "Day", "Active");
        producer.send(new ProducerRecord<>("shift-events", shiftId, objectMapper.writeValueAsString(startEvent)));
        producer.flush();

        // Wait for shift to be created
        Awaitility.await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<?> response = restTemplate.getForEntity(
                            baseUrl + "/shifts/{id}", Object.class, shiftId);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                });

        // Then change status
        ChangeShiftStatusRequested statusEvent = new ChangeShiftStatusRequested(shiftId, "Completed");
        producer.send(new ProducerRecord<>("shift-events", shiftId, objectMapper.writeValueAsString(statusEvent)));
        producer.flush();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<ShiftProjectionResponse> response = restTemplate.getForEntity(
                            baseUrl + "/shifts/{id}", ShiftProjectionResponse.class, shiftId);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().status()).isEqualTo("Completed");
                });
    }

    @Test
    void eventFlow_RecordShiftChangeRequested_KafkaToProjectionToApi_Works() throws Exception {
        String shiftId = "SHIFT-" + UUID.randomUUID();
        String shiftChangeId = "CHANGE-" + UUID.randomUUID();
        Instant startTime = Instant.now();
        
        // First create the shift
        StartShiftRequested startEvent = new StartShiftRequested(shiftId, startTime, null, "Day", "Active");
        producer.send(new ProducerRecord<>("shift-events", shiftId, objectMapper.writeValueAsString(startEvent)));
        producer.flush();

        // Wait for shift to be created
        Awaitility.await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<?> response = restTemplate.getForEntity(
                            baseUrl + "/shifts/{id}", Object.class, shiftId);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                });

        // Then record shift change
        Instant changeTime = Instant.now();
        RecordShiftChangeRequested changeEvent = new RecordShiftChangeRequested(
                shiftId, shiftChangeId, changeTime, "Briefing", "Shift change notes"
        );
        producer.send(new ProducerRecord<>("shift-events", shiftId, objectMapper.writeValueAsString(changeEvent)));
        producer.flush();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<ShiftChangeProjectionResponse> response = restTemplate.getForEntity(
                            baseUrl + "/shift-changes/{id}", ShiftChangeProjectionResponse.class, shiftChangeId);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().shiftChangeId()).isEqualTo(shiftChangeId);
                    assertThat(response.getBody().shiftId()).isEqualTo(shiftId);
                    assertThat(response.getBody().changeType()).isEqualTo("Briefing");
                });
    }

    @Test
    void eventFlow_CheckInOfficerRequested_KafkaToProjectionToApi_Works() throws Exception {
        String shiftId = "SHIFT-" + UUID.randomUUID();
        String badgeNumber = "BADGE-001";
        Instant startTime = Instant.now();
        
        // First create the shift
        StartShiftRequested startEvent = new StartShiftRequested(shiftId, startTime, null, "Day", "Active");
        producer.send(new ProducerRecord<>("shift-events", shiftId, objectMapper.writeValueAsString(startEvent)));
        producer.flush();

        // Wait for shift to be created
        Awaitility.await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<?> response = restTemplate.getForEntity(
                            baseUrl + "/shifts/{id}", Object.class, shiftId);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                });

        // Then check in officer
        Instant checkInTime = Instant.now();
        CheckInOfficerRequested checkInEvent = new CheckInOfficerRequested(
                shiftId, shiftId, badgeNumber, checkInTime, "Regular"
        );
        producer.send(new ProducerRecord<>("officer-shift-events", shiftId, objectMapper.writeValueAsString(checkInEvent)));
        producer.flush();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // Query officer shifts for this shift
                    ResponseEntity<?> response = restTemplate.getForEntity(
                            baseUrl + "/officer-shifts?shiftId=" + shiftId, Object.class);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                });
    }

    @Test
    void eventFlow_CheckOutOfficerRequested_KafkaToProjectionToApi_Works() throws Exception {
        String shiftId = "SHIFT-" + UUID.randomUUID();
        String badgeNumber = "BADGE-001";
        Instant startTime = Instant.now();
        
        // First create the shift and check in
        StartShiftRequested startEvent = new StartShiftRequested(shiftId, startTime, null, "Day", "Active");
        producer.send(new ProducerRecord<>("shift-events", shiftId, objectMapper.writeValueAsString(startEvent)));
        
        Instant checkInTime = Instant.now();
        CheckInOfficerRequested checkInEvent = new CheckInOfficerRequested(
                shiftId, shiftId, badgeNumber, checkInTime, "Regular"
        );
        producer.send(new ProducerRecord<>("officer-shift-events", shiftId, objectMapper.writeValueAsString(checkInEvent)));
        producer.flush();

        // Wait for check-in to be processed
        Awaitility.await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<?> response = restTemplate.getForEntity(
                            baseUrl + "/officer-shifts?shiftId=" + shiftId, Object.class);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                });

        // Then check out officer
        Instant checkOutTime = Instant.now();
        CheckOutOfficerRequested checkOutEvent = new CheckOutOfficerRequested(
                shiftId, shiftId, badgeNumber, checkOutTime
        );
        producer.send(new ProducerRecord<>("officer-shift-events", shiftId, objectMapper.writeValueAsString(checkOutEvent)));
        producer.flush();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<?> response = restTemplate.getForEntity(
                            baseUrl + "/officer-shifts?shiftId=" + shiftId + "&badgeNumber=" + badgeNumber, Object.class);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                });
    }

    @Test
    void eventFlow_UpdateOfficerShiftRequested_KafkaToProjectionToApi_Works() throws Exception {
        String shiftId = "SHIFT-" + UUID.randomUUID();
        String badgeNumber = "BADGE-001";
        Instant startTime = Instant.now();
        
        // First create the shift and check in
        StartShiftRequested startEvent = new StartShiftRequested(shiftId, startTime, null, "Day", "Active");
        producer.send(new ProducerRecord<>("shift-events", shiftId, objectMapper.writeValueAsString(startEvent)));
        
        Instant checkInTime = Instant.now();
        CheckInOfficerRequested checkInEvent = new CheckInOfficerRequested(
                shiftId, shiftId, badgeNumber, checkInTime, "Regular"
        );
        producer.send(new ProducerRecord<>("officer-shift-events", shiftId, objectMapper.writeValueAsString(checkInEvent)));
        producer.flush();

        // Wait for check-in to be processed
        Awaitility.await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<?> response = restTemplate.getForEntity(
                            baseUrl + "/officer-shifts?shiftId=" + shiftId, Object.class);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                });

        // Then update officer shift
        UpdateOfficerShiftRequested updateEvent = new UpdateOfficerShiftRequested(
                shiftId, shiftId, badgeNumber, "Supervisor"
        );
        producer.send(new ProducerRecord<>("officer-shift-events", shiftId, objectMapper.writeValueAsString(updateEvent)));
        producer.flush();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<?> response = restTemplate.getForEntity(
                            baseUrl + "/officer-shifts?shiftId=" + shiftId + "&badgeNumber=" + badgeNumber, Object.class);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                });
    }
}
