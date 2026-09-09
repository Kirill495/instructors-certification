package org.tourism.instructors.application.protocol;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.poi.ss.usermodel.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.tourism.instructors.application.protocol.outbox.ProtocolOutboxWriter;
import org.tourism.publication.contract.AssignmentSnapshot;
import org.tourism.publication.contract.ProtocolSnapshot;
import org.tourism.publication.contract.TopicName;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "outbox.relay.enabled=true")
@Testcontainers
public class ProtocolPublicationIT {

    @Container @ServiceConnection
    static KafkaContainer kafkaContainer = new KafkaContainer("apache/kafka:4.0.0");

    @Container @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired ProtocolOutboxWriter protocolOutboxWriter;
    @Autowired JdbcClient jdbcClient;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("TRUNCATE instructors_grades.published_protocols_outbox").update();
    }

    private ProtocolSnapshot buildProtocol(String pId) {
        LocalDate date = LocalDate.of(2026, 9, 1);
        Instant instant = Instant.parse("2026-09-01T10:00:00Z");
        LocalDate validUntil = date.plusYears(1);
        AssignmentSnapshot aSnapshot =
                new AssignmentSnapshot(
                        1,
                        "Федоров",
                        "Валерий",
                        "А",
                        "инструктор",
                        "горный",
                        "МИЭМ",
                        date,
                        validUntil);
        return new ProtocolSnapshot(
                1, Integer.parseInt(pId), "num", date, "order-num", instant, List.of(aSnapshot));
    }

    @Test
    void testEnqueue_whenTwoSendsWithOneProtocolId_thenTwoRowsShouldExistInOutboxInTheSameOrder() {

        String protocolIdStr = "102";
        ProtocolSnapshot snapshot = buildProtocol(protocolIdStr);
        protocolOutboxWriter.enqueue(protocolIdStr, snapshot);
        protocolOutboxWriter.enqueueTombstone(protocolIdStr);
        try (KafkaConsumer<String, byte[]> consumer =
                new KafkaConsumer<>(
                        Map.of(
                                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                                kafkaContainer.getBootstrapServers(),
                                ConsumerConfig.GROUP_ID_CONFIG,
                                "test-" + UUID.randomUUID(),
                                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                                "earliest",
                                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                                StringDeserializer.class,
                                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                                ByteArrayDeserializer.class))) {
            consumer.subscribe(List.of(TopicName.PROTOCOL_SNAPSHOTS));
            List<ConsumerRecord<String, byte[]>> received = new ArrayList<>();
            await().atMost(Duration.ofSeconds(10))
                    .untilAsserted(
                            () -> {
                                consumer.poll(Duration.ofMillis(200)).forEach(received::add);
                                List<Row> rows = readRows(protocolIdStr);
                                assertEquals(2, rows.size());
                                Row row1 = rows.getFirst();
                                Row row2 = rows.getLast();
                                assertEquals(row2.id, row1.id + 1);
                                assertNotNull(row1.payload);
                                assertNull(row2.payload());
                            });
        }
    }

    @Test
    void sendProtocol_whenSnapshotIsSentToKafka_KafkaRecordNewMessage() {
        String protocolIdStr = "101";
        ProtocolSnapshot snapshot = buildProtocol(protocolIdStr);
        String input =
"""
{"version":1,"protocolId":101,"number":"num","date":"2026-09-01",\
"orderNumber":"order-num","publishedAt":"2026-09-01T10:00:00Z",\
"assignments":[{"rowNum":1,"lastName":"Федоров","firstName":"Валерий","middleName":"А","grade":\
"инструктор","kindOfTourism":"горный","club":"МИЭМ","assignmentDate":"2026-09-01","validUntil":"2027-09-01"}]}\
""";
        protocolOutboxWriter.enqueue(protocolIdStr, snapshot);

        try (KafkaConsumer<String, byte[]> consumer =
                new KafkaConsumer<>(
                        Map.of(
                                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                                kafkaContainer.getBootstrapServers(),
                                ConsumerConfig.GROUP_ID_CONFIG,
                                "test-" + UUID.randomUUID(),
                                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                                "earliest",
                                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                                StringDeserializer.class,
                                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                                ByteArrayDeserializer.class))) {
            consumer.subscribe(List.of(TopicName.PROTOCOL_SNAPSHOTS));
            List<ConsumerRecord<String, byte[]>> received = new ArrayList<>();
            await().atMost(Duration.ofSeconds(10))
                    .untilAsserted(
                            () -> {
                                consumer.poll(Duration.ofMillis(200)).forEach(received::add);
                                Optional<ConsumerRecord<String, byte[]>> firstOpt =
                                        received.stream()
                                                .filter(r -> protocolIdStr.equals(r.key()))
                                                .findFirst();
                                assertTrue(firstOpt.isPresent());
                                ConsumerRecord<String, byte[]> record = firstOpt.get();
                                assertEquals(protocolIdStr, record.key());
                                assertArrayEquals(
                                        input.getBytes(StandardCharsets.UTF_8), record.value());
                                assertNull(record.headers().lastHeader("__TypeId__"));
                                List<Row> rows = readRows(protocolIdStr);
                                assertEquals(1, rows.size());
                                Row row = rows.getFirst();
                                assertEquals(0, row.attempts);
                                assertNotNull(row.sentAt());
                                assertEquals(protocolIdStr, row.messageKey());
                            });
        }
    }

    private List<Row> readRows(String messageKey) {
        return jdbcClient
                .sql(
                        """
                SELECT
                    id, message_key, payload, attempts, sent_at
                FROM
                    instructors_grades.published_protocols_outbox
                WHERE
                    message_key = :message_key
                ORDER BY id
                """)
                .param("message_key", messageKey)
                .query(Row.class)
                .list();
    }

    private record Row(long id, String messageKey, String payload, int attempts, Instant sentAt) {}
}
