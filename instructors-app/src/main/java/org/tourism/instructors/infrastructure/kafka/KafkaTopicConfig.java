package org.tourism.instructors.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.tourism.publication.contract.TopicName;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic protocolsTopic() {

        return TopicBuilder.name(TopicName.PROTOCOL_SNAPSHOTS)
                .partitions(1)
                .replicas(1)
                .compact()
                .build();
    }
}
