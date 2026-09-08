package org.tourism.instructors.application.protocol;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.tourism.instructors.application.protocol.events.ProtocolPublished;
import org.tourism.instructors.application.protocol.events.ProtocolUnpublished;

@Component
public class ProtocolPublicationListener {

    private final ProtocolProducerService protocolProducerService;

    public ProtocolPublicationListener(ProtocolProducerService protocolProducerService) {
        this.protocolProducerService = protocolProducerService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void protocolPublished(ProtocolPublished event) {
        protocolProducerService.sendProtocol(event.snapshot());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void protocolUnpublished(ProtocolUnpublished event) {
        protocolProducerService.sendTombstone(event.protocolId());
    }
}
