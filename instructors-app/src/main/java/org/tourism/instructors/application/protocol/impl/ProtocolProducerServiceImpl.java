package org.tourism.instructors.application.protocol.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.tourism.instructors.application.protocol.ProtocolProducerService;
import org.tourism.instructors.application.protocol.exception.ProtocolPublishException;
import org.tourism.instructors.application.protocol.exception.ProtocolPublishPermanentException;
import org.tourism.instructors.application.protocol.exception.ProtocolPublishRetryableException;
import org.tourism.publication.contract.TopicName;

@Service
@RequiredArgsConstructor
public class ProtocolProducerServiceImpl implements ProtocolProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void send(String key, String payload) throws ProtocolPublishException {
        try {
            kafkaTemplate.send(TopicName.PROTOCOL_SNAPSHOTS, key, payload).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProtocolPublishRetryableException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RetriableException) {
                throw new ProtocolPublishRetryableException(e);
            } else {
                throw new ProtocolPublishPermanentException(e);
            }
        } catch (TimeoutException e) {
            throw new ProtocolPublishRetryableException(e);
        }
    }
}
