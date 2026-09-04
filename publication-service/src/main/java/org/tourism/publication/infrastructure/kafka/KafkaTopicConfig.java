package org.tourism.publication.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.tourism.publication.DltTopicName;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic dltTopic() {
        return TopicBuilder.name(DltTopicName.PROTOCOL_SNAPSHOT_DLT)
                .partitions(1)
                .replicas(1)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, "delete")
                .config(TopicConfig.RETENTION_MS_CONFIG, "86400000")
                .build();
    }
}
