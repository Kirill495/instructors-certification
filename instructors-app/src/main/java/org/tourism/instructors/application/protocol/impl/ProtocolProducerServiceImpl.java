package org.tourism.instructors.application.protocol.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.tourism.instructors.application.protocol.ProtocolProducerService;
import org.tourism.publication.contract.ProtocolSnapshot;
import org.tourism.publication.contract.TopicName;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProtocolProducerServiceImpl implements ProtocolProducerService {

    private final KafkaTemplate<String, ProtocolSnapshot> kafkaTemplate;

    @Override
    public void sendProtocol(ProtocolSnapshot protocolSnapshot) {
        kafkaTemplate
                .send(
                        TopicName.PROTOCOL_SNAPSHOTS,
                        String.valueOf(protocolSnapshot.protocolId()),
                        protocolSnapshot)
                .whenComplete(
                        (result, throwable) -> {
                            if (throwable != null) {
                                log.error(
                                        "Не удалось опубликовать протокол {}",
                                        protocolSnapshot.protocolId(),
                                        throwable);
                            }
                        });
    }

    @Override
    public void sendTombstone(int id) {
        kafkaTemplate
                .send(TopicName.PROTOCOL_SNAPSHOTS, String.valueOf(id), null)
                .whenComplete(
                        (result, throwable) -> {
                            if (throwable != null) {
                                log.error(
                                        "Не удалось опубликовать tombstone протокола {}",
                                        id,
                                        throwable);
                            }
                        });
    }
}
