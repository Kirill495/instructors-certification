package org.tourism.publication.ingest;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.tourism.publication.DltTopicName.PROTOCOL_SNAPSHOT_DLT;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.tourism.publication.contract.TopicName;

@SpringBootTest
@Testcontainers
class ProtocolSnapshotListenerIntegrationTest {

    @Container @ServiceConnection
    static KafkaContainer kafkaContainer = new KafkaContainer("apache/kafka:4.0.0");

    @Container @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    static KafkaProducer<String, String> producer;

    @Autowired JdbcClient jdbcClient;

    @BeforeAll
    static void beforeAll() {
        producer =
                new KafkaProducer<>(
                        Map.of(
                                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                                        kafkaContainer.getBootstrapServers(),
                                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                                        StringSerializer.class));
    }

    @BeforeEach
    void setUp() {
        jdbcClient.sql("TRUNCATE TABLE publication.published_assignments").update();
    }

    @AfterAll
    static void afterAll() {
        producer.close();
    }

    @Test
    void testOnProtocolSnapshot_whenProtocolSnapshotSendToKafka_thenProtocolInsertedInDBTable()
            throws ExecutionException, InterruptedException {

        String protocolNumber = "15";
        String input =
"""
{"version":1,"protocolId":101,"number":"15","date":"2026-03-14","orderNumber":"7","publishedAt":"2026-03-14T10:00:00Z","assignments":[{"rowNum":1,"lastName":"Иванов","firstName":"Пётр","middleName":null,"grade":"3 разряд","kindOfTourism":"горный","club":null,"assignmentDate":"2026-03-14","validUntil":null}]}
""";
        ProducerRecord<String, String> record =
                new ProducerRecord<>(TopicName.PROTOCOL_SNAPSHOTS, "101", input);
        producer.send(record).get();
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            List<RowData> result =
                                    jdbcClient
                                            .sql(
                                                    """
                                    SELECT protocol_id, protocol_number, protocol_date, assignment_date from publication.published_assignments WHERE protocol_id = :protocol_id
                                    """)
                                            .param("protocol_id", 101)
                                            .query(RowData.class)
                                            .list();
                            assertEquals(1, result.size());
                            RowData rowData = result.getFirst();
                            assertEquals(101, rowData.protocolId);
                            assertEquals(protocolNumber, rowData.protocolNumber);
                            assertEquals(LocalDate.of(2026, 3, 14), rowData.protocolDate);
                            assertEquals(LocalDate.of(2026, 3, 14), rowData.assignmentDate);
                        });
    }

    private record RowData(
            int protocolId,
            String protocolNumber,
            LocalDate protocolDate,
            LocalDate assignmentDate) {}

    @Test
    void testOnProtocolSnapshot_whenMalformedPayloadSent_thenExistingRowsKept()
            throws ExecutionException, InterruptedException {

        int protocolId = 101;
        String protocolNumber = "protocol-1";
        insertRow(protocolId, protocolNumber);
        String input =
"""
это не json
""";
        String protocolIdStr = String.valueOf(protocolId);
        ProducerRecord<String, String> record =
                new ProducerRecord<>(TopicName.PROTOCOL_SNAPSHOTS, protocolIdStr, input);

        try (KafkaConsumer<String, byte[]> consumer =
                new KafkaConsumer<>(
                        Map.of(
                                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                                kafkaContainer.getBootstrapServers(),
                                ConsumerConfig.GROUP_ID_CONFIG,
                                "test-dlt-" + UUID.randomUUID(),
                                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                                "earliest",
                                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                                StringDeserializer.class,
                                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                                ByteArrayDeserializer.class))) {
            consumer.subscribe(List.of(PROTOCOL_SNAPSHOT_DLT));
            producer.send(record).get();
            List<ConsumerRecord<String, byte[]>> received = new ArrayList<>();
            await().atMost(Duration.ofSeconds(15))
                    .untilAsserted(
                            () -> {
                                consumer.poll(Duration.ofMillis(200)).forEach(received::add);
                                Optional<ConsumerRecord<String, byte[]>> first =
                                        received.stream()
                                                .filter(r -> protocolIdStr.equals(r.key()))
                                                .findFirst();
                                assertTrue(first.isPresent());
                                ConsumerRecord<String, byte[]> recordResult = first.get();
                                assertEquals(protocolIdStr, recordResult.key());
                                assertArrayEquals(
                                        input.getBytes(StandardCharsets.UTF_8),
                                        recordResult.value());
                                Header header =
                                        recordResult
                                                .headers()
                                                .lastHeader(KafkaHeaders.DLT_EXCEPTION_FQCN);
                                assertNotNull(header);
                                assertEquals(
                                        DeserializationException.class.getName(),
                                        new String(header.value(), StandardCharsets.UTF_8));
                                // Проверим, что запись не была удалена из БД
                                List<String> resProtocolNumbers =
                                        findProtocolNumbersOfProtocolId(protocolId);
                                assertEquals(1, resProtocolNumbers.size());
                                assertEquals(protocolNumber, resProtocolNumbers.getFirst());
                            });
        }
    }

    @Test
    void testOnProtocolSnapshot_whenComeEmptyValue_thenRowShouldBeDeleted()
            throws ExecutionException, InterruptedException {

        int protocolId1 = 101;
        String protocolNumber = "protocol-1";
        int protocolId2 = 102;
        String protocolNumber2 = "protocol-2";
        insertRow(protocolId1, protocolNumber);
        insertRow(protocolId2, protocolNumber2);
        ProducerRecord<String, String> record =
                new ProducerRecord<>(
                        TopicName.PROTOCOL_SNAPSHOTS, String.valueOf(protocolId1), null);
        producer.send(record).get();
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            List<String> pNumbers1 = findProtocolNumbersOfProtocolId(protocolId1);
                            assertTrue(pNumbers1.isEmpty());
                            List<String> pNumbers2 = findProtocolNumbersOfProtocolId(protocolId2);
                            assertFalse(pNumbers2.isEmpty());
                            assertEquals(protocolNumber2, pNumbers2.getFirst());
                        });
    }

    private void insertRow(int protocolId, String protocolNumber) {
        jdbcClient
                .sql(
"""
INSERT INTO publication.published_assignments(protocol_id, protocol_date, order_number, protocol_number, row_num, last_name, first_name, grade, kind_of_tourism, assignment_date)
VALUES(:protocol_id, '2026-09-01', 'order-1', :protocol_number, 1, 'LAST_NAME', 'FIRST_NAME', 'instructor', 'hiking', '2026-09-01')
""")
                .param("protocol_id", protocolId)
                .param("protocol_number", protocolNumber)
                .update();
    }

    private List<String> findProtocolNumbersOfProtocolId(int protocolId) {
        return jdbcClient
                .sql(
                        """
                                SELECT protocol_number FROM publication.published_assignments WHERE protocol_id = :protocol_id
                                """)
                .param("protocol_id", protocolId)
                .query(String.class)
                .list();
    }

    @TestConfiguration
    static class TopicConfig {
        @Bean
        NewTopic protocolSnapshots() {
            return TopicBuilder.name(TopicName.PROTOCOL_SNAPSHOTS)
                    .partitions(1)
                    .replicas(1)
                    .build();
        }
    }
}
