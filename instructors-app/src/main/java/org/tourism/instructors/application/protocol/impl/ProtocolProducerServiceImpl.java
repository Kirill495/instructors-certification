package org.tourism.instructors.application.protocol.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.tourism.instructors.application.protocol.ProtocolProducerService;
import org.tourism.publication.contract.ProtocolSnapshot;
import org.tourism.publication.contract.TopicName;

@Service
@RequiredArgsConstructor
public class ProtocolProducerServiceImpl implements ProtocolProducerService {

    private final KafkaTemplate<String, ProtocolSnapshot> kafkaTemplate;

    @Override
    public void sendProtocol(ProtocolSnapshot protocolSnapshot) {
        kafkaTemplate.send(
                TopicName.PROTOCOL_SNAPSHOTS,
                String.valueOf(protocolSnapshot.protocolId()),
                protocolSnapshot);
    }
}
