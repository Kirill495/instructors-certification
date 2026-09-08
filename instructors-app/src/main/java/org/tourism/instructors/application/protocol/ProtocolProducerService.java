package org.tourism.instructors.application.protocol;

import org.tourism.publication.contract.ProtocolSnapshot;

public interface ProtocolProducerService {
    void sendProtocol(ProtocolSnapshot protocolSnapshot);
}
