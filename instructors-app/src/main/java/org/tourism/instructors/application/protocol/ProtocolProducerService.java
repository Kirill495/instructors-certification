package org.tourism.instructors.application.protocol;

import org.tourism.instructors.application.protocol.exception.ProtocolPublishException;

public interface ProtocolProducerService {

    void send(String key, String payload) throws ProtocolPublishException;
}
