package org.tourism.instructors.application.protocol;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

@SpringBootTest(classes = ProtocolProducerService.class)
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
@ActiveProfiles("test")
@Testcontainers
public class ProtocolServiceKafkaIT {

    @Container
    @ServiceConnection
    static KafkaContainer kafkaContainer = new KafkaContainer("apache/kafka:4.0.0");


}
