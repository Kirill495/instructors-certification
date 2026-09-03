package org.tourism.publication.ingest;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.tourism.publication.contract.ProtocolSnapshot;
import org.tourism.publication.contract.TopicName;
import org.tourism.publication.ingest.exception.IncorrectMessageKeyException;
import org.tourism.publication.ingest.exception.IncorrectProtocolIdException;
import org.tourism.publication.ingest.exception.UnsupportedSnapshotVersionException;

@Component
@RequiredArgsConstructor
public class ProtocolSnapshotListener {

    private static final int SUPPORTED_VERSION = 1;

    private final ProtocolIngestService service;

    @KafkaListener(topics = TopicName.PROTOCOL_SNAPSHOTS)
    public void onProtocolSnapshot(ConsumerRecord<String, ProtocolSnapshot> record) {

        int messageKey;
        try {
            messageKey = Integer.parseInt(record.key());
        } catch (NumberFormatException exception) {
            throw new IncorrectMessageKeyException(
                    "Некорректный ключ записи: {" + record.key() + "}", exception);
        }
        ProtocolSnapshot protocolSnapshot = record.value();

        if (Objects.nonNull(protocolSnapshot)) {
            if (SUPPORTED_VERSION != protocolSnapshot.version()) {
                throw new UnsupportedSnapshotVersionException(
                        "Недопустимая версия записи из messageKey: {"
                                + messageKey
                                + "}. Требуется "
                                + SUPPORTED_VERSION
                                + ". Прочитано: "
                                + protocolSnapshot.version());
            }
            if (messageKey != protocolSnapshot.protocolId()) {
                throw new IncorrectProtocolIdException(
                        "не совпадает protocolId из messageKey: {"
                                + messageKey
                                + "} и protocolSnapshot: "
                                + "{"
                                + protocolSnapshot.protocolId()
                                + "}");
            }
            service.apply(messageKey, protocolSnapshot);
        } else {
            service.remove(messageKey);
        }
    }
}
