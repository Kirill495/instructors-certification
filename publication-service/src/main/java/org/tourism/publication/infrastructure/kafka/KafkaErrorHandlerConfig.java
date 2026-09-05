package org.tourism.publication.infrastructure.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.FixedBackOff;
import org.tourism.publication.ingest.IngestRetryListener;
import org.tourism.publication.ingest.exception.IncorrectMessageKeyException;
import org.tourism.publication.ingest.exception.IncorrectProtocolIdException;

@Configuration
public class KafkaErrorHandlerConfig {
    @Bean
    CommonErrorHandler kafkaErrorHandler(
            DeadLetterPublishingRecoverer recoverer, RetryListener retryListener) {
        FixedBackOff backOff =
                new FixedBackOff(FixedBackOff.DEFAULT_INTERVAL, FixedBackOff.UNLIMITED_ATTEMPTS);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(
                IncorrectMessageKeyException.class, IncorrectProtocolIdException.class);
        handler.setRetryListeners(retryListener);
        return handler;
    }

    @Bean
    RetryListener retryListener() {
        return new IngestRetryListener();
    }
}
