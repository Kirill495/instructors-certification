package org.tourism.instructors.application.protocol.exception;

import lombok.Getter;

@Getter
public class ProtocolNotFoundException extends RuntimeException {

    private final int protocolId;

    public ProtocolNotFoundException(int protocolId) {
        super();
        this.protocolId = protocolId;
    }
}
