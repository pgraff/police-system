package com.knowit.policesystem.common.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for event infrastructure including serialization, deserialization,
 * metadata handling, versioning, and Kafka publishing.
 */
@Testcontainers
class EventInfrastructureTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:latest")
    );

    private ObjectMapper objectMapper;
    private EventPublisher eventPublisher;
    private Consumer<String, String> consumer;
    private String testTopic;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        testTopic = "test-events";

        // Create Kafka producer for EventPublisher
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");

        eventPublisher = new KafkaEventPublisher(producerProps, objectMapper);

        // Create Kafka consumer for verification
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(testTopic));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
        if (eventPublisher instanceof KafkaEventPublisher) {
            ((KafkaEventPublisher) eventPublisher).close();
        }
    }

    @Test
    void testEventSerialization_WithValidEvent_ProducesJson() throws JsonProcessingException {
        // Given
        String aggregateId = "test-aggregate-1";
        String testData = "test data";
        int testNumber = 42;
        TestEvent event = new TestEvent(aggregateId, testData, testNumber);

        // When
        String json = objectMapper.writeValueAsString(event);

        // Then
        assertThat(json).isNotEmpty();
        assertThat(json).contains("eventId");
        assertThat(json).contains("timestamp");
        assertThat(json).contains("aggregateId");
        assertThat(json).contains("version");
        assertThat(json).contains("testData");
        assertThat(json).contains("testNumber");
        assertThat(json).contains(aggregateId);
        assertThat(json).contains(testData);
        assertThat(json).contains(String.valueOf(testNumber));
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(aggregateId);
        assertThat(event.getVersion()).isEqualTo(1);
    }

    @Test
    void testEventDeserialization_WithValidJson_CreatesEvent() throws JsonProcessingException {
        // Given
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        String aggregateId = "test-aggregate-2";
        int version = 1;
        String testData = "deserialized data";
        int testNumber = 100;

        String json = String.format(
                "{\"eventId\":\"%s\",\"timestamp\":\"%s\",\"aggregateId\":\"%s\",\"version\":%d,\"testData\":\"%s\",\"testNumber\":%d}",
                eventId, timestamp.toString(), aggregateId, version, testData, testNumber
        );

        // When
        TestEvent event = objectMapper.readValue(json, TestEvent.class);

        // Then
        assertThat(event).isNotNull();
        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(aggregateId);
        assertThat(event.getVersion()).isEqualTo(version);
        assertThat(event.getTestData()).isEqualTo(testData);
        assertThat(event.getTestNumber()).isEqualTo(testNumber);
        assertThat(event.getEventType()).isEqualTo("TestEvent");
    }

    @Test
    void testEvent_ContainsRequiredMetadata() {
        // Given
        String aggregateId = "test-aggregate-3";
        TestEvent event = new TestEvent(aggregateId, "data", 1);

        // Then
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventId()).isInstanceOf(UUID.class);
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getTimestamp()).isInstanceOf(Instant.class);
        assertThat(event.getAggregateId()).isEqualTo(aggregateId);
        assertThat(event.getVersion()).isEqualTo(1);
        assertThat(event.getEventType()).isEqualTo("TestEvent");
    }

    @Test
    void testEventPublishing_WithValidEvent_PublishesToKafka() throws JsonProcessingException {
        // Given
        String aggregateId = "test-aggregate-4";
        String testData = "kafka test data";
        int testNumber = 200;
        TestEvent event = new TestEvent(aggregateId, testData, testNumber);

        // When
        eventPublisher.publish(testTopic, aggregateId, event);

        // Then - consume and verify
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records).isNotEmpty();
        assertThat(records.count()).isEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        assertThat(record.key()).isEqualTo(aggregateId);
        assertThat(record.topic()).isEqualTo(testTopic);

        // Deserialize and verify event data
        TestEvent consumedEvent = objectMapper.readValue(record.value(), TestEvent.class);
        assertThat(consumedEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(consumedEvent.getAggregateId()).isEqualTo(aggregateId);
        assertThat(consumedEvent.getTestData()).isEqualTo(testData);
        assertThat(consumedEvent.getTestNumber()).isEqualTo(testNumber);
        assertThat(consumedEvent.getVersion()).isEqualTo(1);
    }

    @Test
    void testEventVersioning_WithDifferentVersions_HandlesCorrectly() throws JsonProcessingException {
        // Given
        String aggregateId = "test-aggregate-5";
        TestEvent eventV1 = new TestEvent(aggregateId, 1, "data1", 1);
        TestEvent eventV2 = new TestEvent(aggregateId, 2, "data2", 2);
        TestEvent eventV3 = new TestEvent(aggregateId, 3, "data3", 3);

        // When - serialize
        String jsonV1 = objectMapper.writeValueAsString(eventV1);
        String jsonV2 = objectMapper.writeValueAsString(eventV2);
        String jsonV3 = objectMapper.writeValueAsString(eventV3);

        // Then - verify versions are preserved
        assertThat(jsonV1).contains("\"version\":1");
        assertThat(jsonV2).contains("\"version\":2");
        assertThat(jsonV3).contains("\"version\":3");

        // When - deserialize
        TestEvent deserializedV1 = objectMapper.readValue(jsonV1, TestEvent.class);
        TestEvent deserializedV2 = objectMapper.readValue(jsonV2, TestEvent.class);
        TestEvent deserializedV3 = objectMapper.readValue(jsonV3, TestEvent.class);

        // Then - verify versions are correct
        assertThat(deserializedV1.getVersion()).isEqualTo(1);
        assertThat(deserializedV2.getVersion()).isEqualTo(2);
        assertThat(deserializedV3.getVersion()).isEqualTo(3);
    }

    @Test
    void testEventPublishing_WithCallback_InvokesOnSuccess() throws JsonProcessingException {
        // Given
        String aggregateId = "test-aggregate-6";
        TestEvent event = new TestEvent(aggregateId, "callback test data", 300);
        boolean[] successCalled = {false};
        org.apache.kafka.clients.producer.RecordMetadata[] capturedMetadata = {null};

        EventPublisher.PublishCallback callback = new EventPublisher.PublishCallback() {
            @Override
            public void onSuccess(Event event, org.apache.kafka.clients.producer.RecordMetadata metadata) {
                successCalled[0] = true;
                capturedMetadata[0] = metadata;
            }

            @Override
            public void onFailure(Event event, Exception exception) {
                throw new AssertionError("onFailure should not be called for successful publish", exception);
            }
        };

        // When
        eventPublisher.publish(testTopic, aggregateId, event, callback);

        // Then - wait for async callback
        try {
            Thread.sleep(1000); // Give Kafka time to process
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(successCalled[0]).isTrue();
        assertThat(capturedMetadata[0]).isNotNull();
        assertThat(capturedMetadata[0].topic()).isEqualTo(testTopic);

        // Verify event was actually published
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertThat(records).isNotEmpty();
    }

    @Test
    void testEventPublishing_WithCallback_InvokesOnFailure_WhenSerializationFails() {
        // Given
        String aggregateId = "test-aggregate-7";
        TestEvent event = new TestEvent(aggregateId, "test", 1);
        boolean[] failureCalled = {false};
        Exception[] capturedException = {null};

        EventPublisher.PublishCallback callback = new EventPublisher.PublishCallback() {
            @Override
            public void onSuccess(Event event, org.apache.kafka.clients.producer.RecordMetadata metadata) {
                throw new AssertionError("onSuccess should not be called when serialization fails");
            }

            @Override
            public void onFailure(Event event, Exception exception) {
                failureCalled[0] = true;
                capturedException[0] = exception;
            }
        };

        // Create a publisher with a broken ObjectMapper that will fail serialization
        ObjectMapper brokenMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws com.fasterxml.jackson.core.JsonProcessingException {
                throw new com.fasterxml.jackson.core.JsonProcessingException("Simulated serialization failure") {};
            }
        };
        
        // Create producer properties for the broken publisher
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        
        EventPublisher brokenPublisher = new KafkaEventPublisher(producerProps, brokenMapper);

        // When
        brokenPublisher.publish(testTopic, aggregateId, event, callback);

        // Then - wait a bit for callback
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(failureCalled[0]).isTrue();
        assertThat(capturedException[0]).isNotNull();
        assertThat(capturedException[0].getMessage()).contains("Failed to serialize event to JSON");
        
        // Clean up
        if (brokenPublisher instanceof KafkaEventPublisher) {
            ((KafkaEventPublisher) brokenPublisher).close();
        }
    }
}

