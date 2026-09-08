package org.tourism.instructors.application.protocol;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.tourism.publication.contract.ProtocolSnapshot;

@Component
public class ProtocolSaveEvent {

    private final ProtocolProducerService protocolProducerService;

    public ProtocolSaveEvent(ProtocolProducerService protocolProducerService) {
        this.protocolProducerService = protocolProducerService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void protocolPublished(ProtocolSnapshot snapshot) {
        protocolProducerService.sendProtocol(snapshot);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void protocolUnpublished(int id) {
        protocolProducerService.sendTombstone(id);
    }
}
