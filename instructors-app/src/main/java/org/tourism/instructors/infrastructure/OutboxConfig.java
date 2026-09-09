package org.tourism.instructors.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.JacksonMapperUtils;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class OutboxConfig {

    @Bean
    JsonMapper kafkaJsonMapper() {
        return JacksonMapperUtils.enhancedJsonMapper();
    }
}
