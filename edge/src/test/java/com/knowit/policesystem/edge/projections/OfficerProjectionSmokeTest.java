package com.knowit.policesystem.edge.projections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowit.policesystem.common.events.EventClassification;
import com.knowit.policesystem.common.events.officers.RegisterOfficerRequested;
import com.knowit.policesystem.edge.config.TopicConfiguration;
import com.knowit.policesystem.edge.infrastructure.BaseIntegrationTest;
import com.knowit.policesystem.edge.infrastructure.NatsTestHelper;
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
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end projection smoke test for officer registration.
 * Validates REST -> Kafka/NATS -> projection (Postgres) flow.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class OfficerProjectionSmokeTest extends BaseIntegrationTest {

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("police")
            .withUsername("test")
            .withPassword("test");

    static {
        postgres.start();
    }

    @Autowired
    private MockMvc mockMvc;

    private NatsTestHelper natsHelper;
    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;
    private Consumer<String, String> projectionConsumer;
    private Thread projectionThread;
    private AtomicBoolean projectionRunning;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        var dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS officer_projection (
                    badge_number VARCHAR PRIMARY KEY,
                    first_name VARCHAR,
                    last_name VARCHAR,
                    rank VARCHAR,
                    email VARCHAR,
                    phone_number VARCHAR,
                    hire_date VARCHAR,
                    status VARCHAR
                )
                """);
        jdbcTemplate.execute("TRUNCATE officer_projection");

        natsHelper = new NatsTestHelper(nats.getNatsUrl(), objectMapper);

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "projection-consumer-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        projectionConsumer = new KafkaConsumer<>(consumerProps);
        projectionConsumer.subscribe(Collections.singletonList(TopicConfiguration.OFFICER_EVENTS));
        // Trigger assignment before producing
        projectionConsumer.poll(Duration.ofSeconds(1));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (projectionRunning != null) {
            projectionRunning.set(false);
        }
        if (projectionThread != null) {
            projectionThread.join(2000);
        }
        if (projectionConsumer != null) {
            projectionConsumer.close();
        }
        if (natsHelper != null) {
            natsHelper.close();
        }
        try {
            jdbcTemplate.execute("TRUNCATE officer_projection");
        } catch (Exception ignored) {
        }
    }

    @Test
    void registerOfficer_projectionFlow_persistsToPostgresAndPublishesNats() throws Exception {
        // Arrange
        String badgeNumber = "B-" + UUID.randomUUID();
        String subject = EventClassification.generateNatsSubject(
                new RegisterOfficerRequested(badgeNumber, "Jane", "Doe", "Sergeant",
                        "jane.doe@example.com", "555-1234", "2023-01-01", "Active"));
        natsHelper.ensureStreamForSubject(subject);

        CountDownLatch projectionLatch = new CountDownLatch(1);
        AtomicReference<RegisterOfficerRequested> consumedEvent = new AtomicReference<>();
        projectionRunning = new AtomicBoolean(true);

        projectionThread = new Thread(() -> {
            while (projectionRunning.get()) {
                ConsumerRecords<String, String> records = projectionConsumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        RegisterOfficerRequested event = objectMapper.readValue(record.value(), RegisterOfficerRequested.class);
                        consumedEvent.set(event);
                        upsertProjection(event);
                        projectionLatch.countDown();
                        projectionRunning.set(false);
                        break;
                    } catch (Exception e) {
                        // ignore and retry
                    }
                }
            }
        });
        projectionThread.start();

        String payload = """
                {
                  "badgeNumber": "%s",
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "rank": "Sergeant",
                  "email": "jane.doe@example.com",
                  "phoneNumber": "555-1234",
                  "hireDate": "2023-01-01",
                  "status": "Active"
                }
                """.formatted(badgeNumber);

        // Act: REST call
        mockMvc.perform(post("/api/v1/officers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        // Assert: Kafka -> projection -> Postgres
        boolean projectionCompleted = projectionLatch.await(30, TimeUnit.SECONDS);
        assertThat(projectionCompleted).as("projection should process event").isTrue();
        assertThat(consumedEvent.get()).isNotNull();
        assertThat(consumedEvent.get().getBadgeNumber()).isEqualTo(badgeNumber);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT badge_number, first_name, last_name, rank, email, phone_number, hire_date, status FROM officer_projection WHERE badge_number = ?",
                badgeNumber);
        assertThat(row.get("badge_number")).isEqualTo(badgeNumber);
        assertThat(row.get("first_name")).isEqualTo("Jane");
        assertThat(row.get("last_name")).isEqualTo("Doe");
        assertThat(row.get("rank")).isEqualTo("Sergeant");
        assertThat(row.get("email")).isEqualTo("jane.doe@example.com");
        assertThat(row.get("phone_number")).isEqualTo("555-1234");
        assertThat(row.get("hire_date")).isEqualTo("2023-01-01");
        assertThat(row.get("status")).isEqualTo("Active");

        // Assert: NATS received the critical event
        String natsMessage = natsHelper.consumeMessage(subject, Duration.ofSeconds(10));
        assertThat(natsMessage).isNotNull();

        projectionRunning.set(false);
        projectionThread.join(5000);
    }

    private void upsertProjection(RegisterOfficerRequested event) throws SQLException {
        jdbcTemplate.update("""
                INSERT INTO officer_projection (badge_number, first_name, last_name, rank, email, phone_number, hire_date, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (badge_number) DO UPDATE SET
                    first_name = EXCLUDED.first_name,
                    last_name = EXCLUDED.last_name,
                    rank = EXCLUDED.rank,
                    email = EXCLUDED.email,
                    phone_number = EXCLUDED.phone_number,
                    hire_date = EXCLUDED.hire_date,
                    status = EXCLUDED.status
                """,
                event.getBadgeNumber(),
                event.getFirstName(),
                event.getLastName(),
                event.getRank(),
                event.getEmail(),
                event.getPhoneNumber(),
                event.getHireDate(),
                event.getStatus());
    }
}
