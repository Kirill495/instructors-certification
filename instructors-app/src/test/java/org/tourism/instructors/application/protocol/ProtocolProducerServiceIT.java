package org.tourism.instructors.application.protocol;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.tourism.instructors.application.protocol.impl.ProtocolProducerServiceImpl;
import org.tourism.instructors.infrastructure.kafka.KafkaTopicConfig;
import org.tourism.publication.contract.AssignmentSnapshot;
import org.tourism.publication.contract.ProtocolSnapshot;
import org.tourism.publication.contract.TopicName;

@SpringBootTest(classes = {ProtocolProducerServiceImpl.class, KafkaTopicConfig.class})
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
@ActiveProfiles("test")
@Testcontainers
public class ProtocolProducerServiceIT {

    @Container @ServiceConnection
    static KafkaContainer kafkaContainer = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired ProtocolProducerService protocolProducerService;

    @Test
    void sendProtocol_whenSnapshotIsSentToKafka_KafkaRecordNewMessage() {
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
        int protocolId = 101;
        String protocolIdStr = String.valueOf(protocolId);
        ProtocolSnapshot snapshot =
                new ProtocolSnapshot(
                        1, protocolId, "num", date, "order-num", instant, List.of(aSnapshot));
        String input =
"""
{"version":1,"protocolId":101,"number":"num","date":"2026-09-01",\
"orderNumber":"order-num","publishedAt":"2026-09-01T10:00:00Z",\
"assignments":[{"rowNum":1,"lastName":"Федоров","firstName":"Валерий","middleName":"А","grade":\
"инструктор","kindOfTourism":"горный","club":"МИЭМ","assignmentDate":"2026-09-01","validUntil":"2027-09-01"}]}\
""";
        protocolProducerService.sendProtocol(snapshot);
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
                            });
        }
    }
}
